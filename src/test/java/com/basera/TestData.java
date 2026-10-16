package com.basera;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.basera.model.Category;
import com.basera.model.Listing;
import com.basera.model.Role;
import com.basera.model.User;
import com.basera.repository.BookingRepository;
import com.basera.repository.ListingRepository;
import com.basera.repository.ReviewRepository;
import com.basera.repository.UserRepository;
import com.basera.security.CurrentUser;

/** Small helpers that put test users and listings in the database. */
@Component
public class TestData {

    public static final String PASSWORD = "password123";

    private final UserRepository users;
    private final ListingRepository listings;
    private final ReviewRepository reviews;
    private final BookingRepository bookings;
    private final PasswordEncoder passwordEncoder;

    public TestData(UserRepository users, ListingRepository listings, ReviewRepository reviews,
            BookingRepository bookings, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.listings = listings;
        this.reviews = reviews;
        this.bookings = bookings;
        this.passwordEncoder = passwordEncoder;
    }

    /** Empties every collection, so each test starts from nothing. */
    public void clear() {
        bookings.deleteAll();
        reviews.deleteAll();
        listings.deleteAll();
        users.deleteAll();
    }

    public User user(String username, Role role) {
        return users.save(new User(username, username + "@test.dev", passwordEncoder.encode(PASSWORD), role));
    }

    public Listing listing(User owner, int price, int maxGuests) {
        Listing listing = new Listing();
        listing.setTitle("Test stay " + price);
        listing.setLocation("Pune");
        listing.setCountry("India");
        listing.setPrice(price);
        listing.setMaxGuests(maxGuests);
        listing.setCategory(Category.ROOMS);
        listing.setOwner(owner);
        return listings.save(listing);
    }

    public static CurrentUser login(User user) {
        return new CurrentUser(user);
    }
}
