package com.basera.seed;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.basera.model.Booking;
import com.basera.model.Category;
import com.basera.model.Image;
import com.basera.model.Listing;
import com.basera.model.Review;
import com.basera.model.Role;
import com.basera.model.User;
import com.basera.repository.BookingRepository;
import com.basera.repository.ListingRepository;
import com.basera.repository.ReviewRepository;
import com.basera.repository.UserRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Fills the database with demo users, listings, reviews and bookings (seeds/seed.js).
 *
 * A CommandLineRunner runs once, right after the app has started. What it does depends on `basera.seed`
 * in application.properties:
 *   if-empty (default)  seed only when there are no users yet - covers both a fresh local database
 *                       and a brand-new Atlas database on Render, so no manual `npm run seed` step
 *   reset               delete everything and re-seed on every start (the old `npm run seed`)
 *   never               do nothing
 *
 * Demo accounts (password for all: password123)
 *   Host:     demohost, priyahost
 *   Customer: democustomer, rahul
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public static final String DEMO_PASSWORD = "password123";

    private static final List<User> DEMO_USERS = List.of(
            new User("demohost", "host@basera.dev", null, Role.HOST),
            new User("priyahost", "priya@basera.dev", null, Role.HOST),
            new User("democustomer", "customer@basera.dev", null, Role.CUSTOMER),
            new User("rahul", "rahul@basera.dev", null, Role.CUSTOMER));

    private record SampleReview(int rating, String comment) {
    }

    private static final List<SampleReview> SAMPLE_REVIEWS = List.of(
            new SampleReview(5, "Absolutely loved it - spotless, great host and an unbeatable view."),
            new SampleReview(4, "Lovely place and very comfortable. Check-in was a little slow."),
            new SampleReview(5, "Exactly like the photos. Would book again in a heartbeat!"),
            new SampleReview(3, "Good location, but the wifi was patchy."));

    /** The shape of each entry in src/main/resources/seed/listings.json (converted from seeds/listings.js). */
    record SeedListing(String title, String description, Image image, int price, String location,
            String country, Category category, int maxGuests, SeedGeometry geometry) {
    }

    record SeedGeometry(String type, double[] coordinates) {
    }

    // The objects this class needs. Spring passes them into the constructor when it creates the seeder -
    // this is "dependency injection", and it replaces require(...) for shared objects.
    private final UserRepository users;
    private final ListingRepository listings;
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final PasswordEncoder passwordEncoder;
    private final JsonMapper jsonMapper;
    private final String mode;

    public DataSeeder(UserRepository users, ListingRepository listings, ReviewRepository reviews,
            BookingRepository bookings, PasswordEncoder passwordEncoder, JsonMapper jsonMapper,
            @Value("${basera.seed}") String mode) {
        this.users = users;
        this.listings = listings;
        this.reviews = reviews;
        this.bookings = bookings;
        this.passwordEncoder = passwordEncoder;
        this.jsonMapper = jsonMapper;
        this.mode = mode;
    }

    @Override
    public void run(String... args) throws Exception {
        switch (mode) {
            case "never" -> {
                return;
            }
            case "if-empty" -> {
                if (users.count() > 0) {
                    log.info("Database already has data - skipping demo seed.");
                    return;
                }
            }
            case "reset" -> log.info("basera.seed=reset - deleting all data and re-seeding.");
            default -> throw new IllegalStateException("basera.seed must be if-empty, reset or never, not: " + mode);
        }
        seed();
    }

    private void seed() throws Exception {
        bookings.deleteAll();
        reviews.deleteAll();
        listings.deleteAll();
        users.deleteAll();

        // Users - hash the shared demo password for each one.
        List<User> savedUsers = new ArrayList<>();
        for (User demo : DEMO_USERS) {
            String hash = passwordEncoder.encode(DEMO_PASSWORD);
            savedUsers.add(users.save(new User(demo.getUsername(), demo.getEmail(), hash, demo.getRole())));
        }
        List<User> hosts = savedUsers.stream().filter(User::isHost).toList();
        List<User> customers = savedUsers.stream().filter(User::isCustomer).toList();

        // Listings - alternate between the two hosts; give roughly two thirds of them one or two reviews.
        List<Listing> newListings = new ArrayList<>();
        List<SeedListing> sample = readSampleListings();
        for (int i = 0; i < sample.size(); i++) {
            Listing listing = toListing(sample.get(i));
            listing.setOwner(hosts.get(i % hosts.size()));

            if (i % 3 != 2) {
                int reviewCount = (i % 2) + 1;
                for (int r = 0; r < reviewCount; r++) {
                    SampleReview text = SAMPLE_REVIEWS.get((i + r) % SAMPLE_REVIEWS.size());
                    User author = customers.get(r % customers.size());
                    listing.getReviews().add(reviews.save(new Review(text.comment(), text.rating(), author)));
                }
            }
            newListings.add(listing);
        }
        List<Listing> savedListings = listings.saveAll(newListings);

        // Bookings - one upcoming and one past trip for democustomer, one upcoming for rahul.
        User democustomer = customers.get(0);
        User rahul = customers.get(1);
        bookings.saveAll(List.of(
                demoBooking(savedListings.get(0), democustomer, 10, 3),
                demoBooking(savedListings.get(3), democustomer, -30, 2),
                demoBooking(savedListings.get(1), rahul, 5, 4)));

        log.info("Seeded {} users, {} listings and 3 bookings.", savedUsers.size(), savedListings.size());
        log.info("Demo logins (password \"{}\"): host -> demohost | customer -> democustomer", DEMO_PASSWORD);
    }

    private List<SeedListing> readSampleListings() throws Exception {
        try (InputStream json = new ClassPathResource("seed/listings.json").getInputStream()) {
            return List.of(jsonMapper.readValue(json, SeedListing[].class));
        }
    }

    private Listing toListing(SeedListing seed) {
        Listing listing = new Listing();
        listing.setTitle(seed.title());
        listing.setDescription(seed.description());
        listing.setImage(seed.image());
        listing.setPrice(seed.price());
        listing.setLocation(seed.location());
        listing.setCountry(seed.country());
        listing.setCategory(seed.category());
        listing.setMaxGuests(seed.maxGuests());
        if (seed.geometry() != null) {
            double[] lngLat = seed.geometry().coordinates();
            listing.setGeometry(new GeoJsonPoint(lngLat[0], lngLat[1]));
        }
        return listing;
    }

    /** A confirmed stay starting `startDay` days from today (negative = in the past). */
    private Booking demoBooking(Listing listing, User guest, int startDay, int nights) {
        Booking booking = new Booking();
        booking.setListing(listing);
        booking.setGuest(guest);
        booking.setCheckIn(LocalDate.now().plusDays(startDay));
        booking.setCheckOut(LocalDate.now().plusDays(startDay + nights));
        booking.setGuests(2);
        booking.setPricePerNight(listing.getPrice());
        booking.setTotalPrice((int) Math.round(listing.getPrice() * nights * 1.1)); // nights x price + 10% service fee
        return booking;
    }
}
