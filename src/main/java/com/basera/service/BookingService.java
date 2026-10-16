package com.basera.service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.basera.dto.BookingForm;
import com.basera.exception.AppException;
import com.basera.model.Booking;
import com.basera.model.BookingStatus;
import com.basera.model.Listing;
import com.basera.model.User;
import com.basera.repository.BookingRepository;
import com.basera.repository.ListingRepository;
import com.basera.repository.UserRepository;
import com.basera.security.CurrentUser;

/**
 * Booking business logic - shared by the website and the API (services/bookingService.js).
 *
 * Rules enforced here:
 *   1. You can't book dates in the past.
 *   2. You can't book more guests than the listing allows.
 *   3. A host can't book their own listing.
 *   4. Two confirmed bookings for the same listing can never overlap (409 Conflict).
 *   5. Price = nights x price-per-night + service fee, calculated on the server (never trusted from the client).
 */
@Service
public class BookingService {

    public static final double SERVICE_FEE_RATE = 0.1; // 10% platform fee shown on the price breakdown

    /** calculatePrice's return value. A record: a small class that only holds these four values. */
    public record PriceBreakdown(long nights, long subtotal, long serviceFee, long total) {
    }

    /**
     * What canAccessBooking (middleware/auth.js) found: the booking, and whether the current user is its guest
     * (the other possibility being the host of the listing).
     */
    public record BookingAccess(Booking booking, boolean isGuest) {
    }

    private final BookingRepository bookings;
    private final ListingRepository listings;
    private final UserRepository users;
    private final MongoTemplate mongo;

    public BookingService(BookingRepository bookings, ListingRepository listings, UserRepository users, MongoTemplate mongo) {
        this.bookings = bookings;
        this.listings = listings;
        this.users = users;
        this.mongo = mongo;
    }

    /** Works out the price breakdown for a stay. */
    public static PriceBreakdown calculatePrice(int pricePerNight, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        long subtotal = nights * pricePerNight;
        long serviceFee = Math.round(subtotal * SERVICE_FEE_RATE);
        return new PriceBreakdown(nights, subtotal, serviceFee, subtotal + serviceFee);
    }

    /** CREATE - a customer books a listing. */
    public Booking create(BookingForm form, CurrentUser guest) {
        if (form.getListing() == null) {
            throw AppException.badRequest("listing is required");
        }
        Listing listing = listings.findById(form.getListing())
                .orElseThrow(() -> AppException.notFound("Listing not found.", null));
        if (listing.getOwner().getId().equals(guest.getId())) {
            throw AppException.badRequest("You can't book your own listing.");
        }
        assertBookable(listing, form, null);

        Booking booking = new Booking();
        booking.setListing(listing);
        booking.setGuest(users.findById(guest.getId()).orElseThrow());
        booking.setCheckIn(form.getCheckIn());
        booking.setCheckOut(form.getCheckOut());
        booking.setGuests(form.guestsOrDefault());
        booking.setPricePerNight(listing.getPrice());
        booking.setTotalPrice((int) calculatePrice(listing.getPrice(), form.getCheckIn(), form.getCheckOut()).total());
        return bookings.save(booking);
    }

    /** READ (many) - a customer's trips, newest check-in first. */
    public List<Booking> forGuest(String guestId) {
        User guest = users.findById(guestId).orElseThrow();
        return mongo.find(new Query(where("guest").is(guest)).with(Sort.by(Sort.Direction.DESC, "checkIn")), Booking.class);
    }

    /** READ (many) - every booking made on any of a host's listings. */
    public List<Booking> forHost(String hostId) {
        User host = users.findById(hostId).orElseThrow();
        List<Listing> hostListings = mongo.find(new Query(where("owner").is(host)), Listing.class);
        return mongo.find(new Query(where("listing").in(hostListings)).with(Sort.by(Sort.Direction.DESC, "checkIn")), Booking.class);
    }

    /** READ - date ranges already taken for a listing (shown on the listing page). */
    public List<Booking> upcomingBookedRanges(Listing listing) {
        return mongo.find(new Query(where("listing").is(listing)
                .and("status").is(BookingStatus.CONFIRMED)
                .and("checkOut").gt(LocalDate.now()))
                .with(Sort.by(Sort.Direction.ASC, "checkIn")), Booking.class);
    }

    /**
     * canAccessBooking: a booking can be seen/cancelled by the customer who made it and by the host who owns
     * the listing. Everyone else gets 403.
     */
    public BookingAccess requireAccess(String id, CurrentUser user) {
        Booking booking = bookings.findById(id)
                .orElseThrow(() -> AppException.notFound("Booking not found.", "/bookings"));
        boolean isGuest = booking.getGuest() != null && booking.getGuest().getId().equals(user.getId());
        boolean isHostOfListing = booking.getListing() != null && booking.getListing().getOwner().getId().equals(user.getId());
        if (!isGuest && !isHostOfListing) {
            throw AppException.forbidden("You don't have access to this booking.", "/bookings");
        }
        return new BookingAccess(booking, isGuest);
    }

    /** canAccessBooking + isBookingGuest: only the customer who made the booking may change its dates. */
    public Booking requireGuest(String id, CurrentUser user) {
        BookingAccess access = requireAccess(id, user);
        if (!access.isGuest()) {
            throw AppException.forbidden("Only the guest who made this booking can change it.", "/dashboard");
        }
        return access.booking();
    }

    /** UPDATE - the guest changes dates or number of guests. The price is recalculated. */
    public Booking update(Booking booking, BookingForm form) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw AppException.badRequest("A cancelled booking can't be changed.");
        }
        assertBookable(booking.getListing(), form, booking.getId());

        booking.setCheckIn(form.getCheckIn());
        booking.setCheckOut(form.getCheckOut());
        booking.setGuests(form.guestsOrDefault());
        booking.setTotalPrice((int) calculatePrice(booking.getPricePerNight(), form.getCheckIn(), form.getCheckOut()).total());
        return bookings.save(booking);
    }

    /**
     * DELETE - cancels a booking. A "soft delete": the record stays, marked cancelled, so the guest and
     * the host both keep a history of it. (Listings, by contrast, are really deleted.)
     */
    public Booking cancel(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw AppException.badRequest("This booking is already cancelled.");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return bookings.save(booking);
    }

    /** Part of deleting a listing: its bookings go too. */
    public void deleteAllFor(Listing listing) {
        mongo.remove(new Query(where("listing").is(listing)), Booking.class);
    }

    /** Throws a 400/409 error if the requested stay breaks any booking rule. */
    private void assertBookable(Listing listing, BookingForm form, String ignoreBookingId) {
        if (form.getCheckIn().isBefore(LocalDate.now())) {
            throw AppException.badRequest("Check-in date can't be in the past.");
        }
        if (form.guestsOrDefault() > listing.getMaxGuests()) {
            throw AppException.badRequest("This place allows at most " + listing.getMaxGuests() + " guests.");
        }
        ObjectId ignore = ignoreBookingId == null ? null : new ObjectId(ignoreBookingId);
        if (bookings.existsOverlapping(new ObjectId(listing.getId()), form.getCheckIn(), form.getCheckOut(), ignore)) {
            throw new AppException(HttpStatus.CONFLICT, "Sorry, those dates are already booked. Please pick different dates.");
        }
    }
}
