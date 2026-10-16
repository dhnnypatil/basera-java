package com.basera.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.basera.model.Booking;
import com.basera.model.Image;
import com.basera.model.Listing;
import com.basera.model.Review;
import com.basera.model.User;
import com.basera.security.CurrentUser;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The JSON shapes the API sends back ("response DTOs").
 *
 * Why not send the model objects directly? A Listing holds its owner, a full User, and that includes the
 * password hash. The Node app solved this with toPublicJSON(). Here each response is a record listing exactly
 * the fields to send, so nothing private can slip out. Jackson (the JSON library) turns a record into an
 * object with one property per component; @JsonProperty("_id") keeps the Node app's `_id` name.
 */
public final class ApiJson {

    private ApiJson() {
    }

    /** toPublicJSON() in models/user.js */
    public record UserJson(@JsonProperty("_id") String id, String username, String email, String role) {

        public static UserJson from(User user) {
            return new UserJson(user.getId(), user.getUsername(), user.getEmail(), user.getRole().getParam());
        }

        public static UserJson from(CurrentUser user) {
            return new UserJson(user.getId(), user.getUsername(), user.getEmail(), user.getRole().getParam());
        }
    }

    /** A user shown inside another object: the listing's owner, a review's author. */
    public record UserRef(@JsonProperty("_id") String id, String username) {

        static UserRef from(User user) {
            return user == null ? null : new UserRef(user.getId(), user.getUsername());
        }
    }

    public record GeometryJson(String type, List<Double> coordinates) {
    }

    public record ReviewJson(@JsonProperty("_id") String id, int rating, String comment, UserRef author, Instant createdAt) {

        static ReviewJson from(Review review) {
            return new ReviewJson(review.getId(), review.getRating(), review.getComment(),
                    UserRef.from(review.getAuthor()), review.getCreatedAt());
        }
    }

    public record ListingJson(@JsonProperty("_id") String id, String title, String description, Image image,
            int price, String location, String country, String category, int maxGuests, UserRef owner,
            List<ReviewJson> reviews, Double averageRating, GeometryJson geometry, Instant createdAt, Instant updatedAt) {

        public static ListingJson from(Listing l) {
            GeometryJson geometry = l.getGeometry() == null ? null
                    : new GeometryJson("Point", List.of(l.getGeometry().getX(), l.getGeometry().getY()));
            List<ReviewJson> reviews = l.getReviews().stream().filter(Objects::nonNull).map(ReviewJson::from).toList();
            return new ListingJson(l.getId(), l.getTitle(), l.getDescription(), l.getImage(), l.getPrice(),
                    l.getLocation(), l.getCountry(), l.getCategory().getLabel(), l.getMaxGuests(),
                    UserRef.from(l.getOwner()), reviews, l.getAverageRating(), geometry, l.getCreatedAt(), l.getUpdatedAt());
        }
    }

    /** The listing as shown inside a booking (the Node app's populate("listing", "title location country image price")). */
    public record ListingSummary(@JsonProperty("_id") String id, String title, String location, String country,
            Image image, int price) {

        static ListingSummary from(Listing l) {
            return l == null ? null : new ListingSummary(l.getId(), l.getTitle(), l.getLocation(), l.getCountry(), l.getImage(), l.getPrice());
        }
    }

    /** The guest as shown inside a booking (populate("guest", "username email")). */
    public record GuestSummary(@JsonProperty("_id") String id, String username, String email) {

        static GuestSummary from(User u) {
            return u == null ? null : new GuestSummary(u.getId(), u.getUsername(), u.getEmail());
        }
    }

    public record BookingJson(@JsonProperty("_id") String id, ListingSummary listing, GuestSummary guest,
            LocalDate checkIn, LocalDate checkOut, int guests, int pricePerNight, int totalPrice, String status,
            long nights, Instant createdAt, Instant updatedAt) {

        public static BookingJson from(Booking b) {
            return new BookingJson(b.getId(), ListingSummary.from(b.getListing()), GuestSummary.from(b.getGuest()),
                    b.getCheckIn(), b.getCheckOut(), b.getGuests(), b.getPricePerNight(), b.getTotalPrice(),
                    b.getStatus().name().toLowerCase(), b.getNights(), b.getCreatedAt(), b.getUpdatedAt());
        }
    }
}
