package com.basera.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.basera.dto.ListingFilters;
import com.basera.dto.ListingForm;
import com.basera.exception.AppException;
import com.basera.model.Booking;
import com.basera.model.BookingStatus;
import com.basera.model.Category;
import com.basera.model.Image;
import com.basera.model.Listing;
import com.basera.model.Review;
import com.basera.model.User;
import com.basera.repository.ListingRepository;
import com.basera.repository.ReviewRepository;
import com.basera.repository.UserRepository;
import com.basera.security.CurrentUser;

/**
 * Listing business logic (services/listingService.js).
 *
 * Both the website controllers (web/ListingController) and the API (api/ListingApiController) call this class,
 * so the rules live in one place. Like the Node services, it never touches the request or response:
 * it takes plain values and returns data, or throws an AppException.
 *
 * MongoTemplate is Spring Data's general-purpose database object, for queries too dynamic for a repository
 * method name - here, a search where every filter is optional. Criteria/Query build the same filter object
 * you'd write by hand in Mongoose: where("price").gte(1000)  ->  { price: { $gte: 1000 } }.
 */
@Service
public class ListingService {

    private final ListingRepository listings;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final BookingService bookingService;
    private final GeocodeService geocoder;
    private final MongoTemplate mongo;

    public ListingService(ListingRepository listings, ReviewRepository reviews, UserRepository users,
            BookingService bookingService, GeocodeService geocoder, MongoTemplate mongo) {
        this.listings = listings;
        this.reviews = reviews;
        this.users = users;
        this.bookingService = bookingService;
        this.geocoder = geocoder;
        this.mongo = mongo;
    }

    /**
     * READ (many) - search listings. Every filter is optional (see dto/ListingFilters):
     * q (title/location/country), category, minPrice/maxPrice, guests, checkIn/checkOut (only free listings),
     * sort = newest | price_asc | price_desc | rating.
     */
    public List<Listing> search(ListingFilters filters) {
        Query query = new Query();

        if (filters.getQ() != null) {
            // new RegExp(escapeRegex(q), "i"): escaping stops ".*" or "(" in the search box acting as regex syntax.
            Pattern pattern = Pattern.compile(escapeRegex(filters.getQ()), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    where("title").regex(pattern), where("location").regex(pattern), where("country").regex(pattern)));
        }
        if (filters.getCategory() != null) {
            Category category = Category.fromParam(filters.getCategory());
            if (category == null) {
                return List.of(); // unknown category: nothing matches
            }
            query.addCriteria(where("category").is(category));
        }
        if (filters.getGuests() != null) {
            query.addCriteria(where("maxGuests").gte(filters.getGuests()));
        }
        if (filters.getMinPrice() != null || filters.getMaxPrice() != null) {
            Criteria price = where("price");
            if (filters.getMinPrice() != null) {
                price.gte(filters.getMinPrice());
            }
            if (filters.getMaxPrice() != null) {
                price.lte(filters.getMaxPrice());
            }
            query.addCriteria(price);
        }

        // Availability: hide listings that already have a booking overlapping the requested dates.
        if (filters.getCheckIn() != null && filters.getCheckOut() != null) {
            List<ObjectId> booked = mongo.findDistinct(
                    new Query(where("status").is(BookingStatus.CONFIRMED)
                            .and("checkIn").lt(filters.getCheckOut())
                            .and("checkOut").gt(filters.getCheckIn())),
                    "listing", Booking.class, ObjectId.class);
            query.addCriteria(where("_id").nin(booked));
        }

        String sort = filters.getSort() == null ? "newest" : filters.getSort();
        query.with(switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // newest (also the base order for "rating")
        });
        List<Listing> results = mongo.find(query, Listing.class);

        // The rating is calculated from the reviews (a "virtual"), so it can't be sorted in the database query.
        if ("rating".equals(sort)) {
            results = new ArrayList<>(results);
            results.sort(Comparator.comparingDouble((Listing l) -> l.getAverageRating() == null ? 0 : l.getAverageRating()).reversed());
        }
        return results;
    }

    /** READ (many) - the best-rated listings, for the home page. */
    public List<Listing> featured(int limit) {
        return search(ListingFilters.sortedBy("rating")).stream().limit(limit).toList();
    }

    /** READ (many) - all listings owned by one host, newest first (host dashboard). */
    public List<Listing> byOwner(String ownerId) {
        User owner = users.findById(ownerId).orElseThrow();
        return mongo.find(new Query(where("owner").is(owner)).with(Sort.by(Sort.Direction.DESC, "createdAt")), Listing.class);
    }

    /** READ (one) - a single listing with its owner and reviews, newest review first. */
    public Listing getById(String id) {
        Listing listing = listings.findById(id)
                .orElseThrow(() -> AppException.notFound("Listing not found.", null));
        List<Review> newestFirst = new ArrayList<>(listing.getReviews());
        newestFirst.removeIf(Objects::isNull); // a review id whose review was deleted
        newestFirst.sort(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        listing.setReviews(newestFirst);
        return listing;
    }

    /**
     * isListingOwner (middleware/auth.js): only the host who created a listing may edit or delete it.
     * Returns the listing so the controller doesn't load it twice.
     */
    public Listing requireOwned(String id, CurrentUser user) {
        Listing listing = listings.findById(id)
                .orElseThrow(() -> AppException.notFound("Listing not found.", "/listings"));
        if (!listing.getOwner().getId().equals(user.getId())) {
            throw AppException.forbidden("You are not the owner of this listing.", "/listings/" + id);
        }
        return listing;
    }

    /** CREATE - a new listing owned by `owner`. `uploaded` is the stored photo, or null. */
    public Listing create(ListingForm form, CurrentUser owner, Image uploaded) {
        Listing listing = new Listing();
        listing.setOwner(users.findById(owner.getId()).orElseThrow());
        applyForm(listing, form);
        listing.setImage(uploaded != null ? uploaded : new Image(form.getPastedImageUrl(), null));
        listing.setGeometry(geocoder.geocode(listing.getLocation() + ", " + listing.getCountry()));
        return listings.save(listing);
    }

    /**
     * UPDATE - change an existing listing (ownership already checked by requireOwned).
     * Keeps the current photo if no new file or URL was given, and re-geocodes only if the address changed.
     */
    public Listing update(Listing listing, ListingForm form, Image uploaded) {
        boolean addressChanged = !form.getLocation().trim().equals(listing.getLocation())
                || !form.getCountry().trim().equals(listing.getCountry());
        applyForm(listing, form);
        if (uploaded != null) {
            listing.setImage(uploaded);
        } else if (form.getPastedImageUrl() != null) {
            listing.setImage(new Image(form.getPastedImageUrl(), null));
        }
        if (addressChanged) {
            listing.setGeometry(geocoder.geocode(listing.getLocation() + ", " + listing.getCountry()));
        }
        return listings.save(listing);
    }

    /**
     * DELETE - removes the listing and, with it, its reviews and bookings.
     * (In Node this cascade was a Mongoose post("findOneAndDelete") hook on the model.)
     */
    public void delete(Listing listing) {
        reviews.deleteAll(listing.getReviews().stream().filter(Objects::nonNull).toList());
        bookingService.deleteAllFor(listing);
        listings.delete(listing);
    }

    /** Copies the validated form fields onto the listing (Mongoose's `listing.set(fields)`), trimmed like the schema. */
    private void applyForm(Listing listing, ListingForm form) {
        listing.setTitle(form.getTitle().trim());
        listing.setDescription(form.getDescription() == null ? null : form.getDescription().trim());
        listing.setLocation(form.getLocation().trim());
        listing.setCountry(form.getCountry().trim());
        listing.setPrice(form.getPrice());
        listing.setCategory(Category.fromParam(form.getCategory()));
        listing.setMaxGuests(form.getMaxGuests() == null ? 2 : form.getMaxGuests());
    }

    /** Escapes characters that have a special meaning in a regular expression. */
    static String escapeRegex(String text) {
        return text.replaceAll("[.*+?^${}()|\\[\\]\\\\]", "\\\\$0");
    }
}
