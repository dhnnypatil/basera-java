package com.basera.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.basera.TestData;
import com.basera.TestMongo;
import com.basera.dto.BookingForm;
import com.basera.dto.ListingFilters;
import com.basera.dto.ReviewForm;
import com.basera.exception.AppException;
import com.basera.model.Booking;
import com.basera.model.BookingStatus;
import com.basera.model.Listing;
import com.basera.model.Role;
import com.basera.model.User;
import com.basera.repository.BookingRepository;
import com.basera.repository.ReviewRepository;

/**
 * The booking rules from services/bookingService.js, checked against a real MongoDB (so the overlap query
 * and the other database queries are tested too, not just the Java logic).
 *
 * @SpringBootTest starts the whole app (without the web server). @Autowired asks Spring for a ready-made
 * object, like the constructor injection in the app's own classes. Each @Test method is one test case;
 * assertThat(...) checks a result and fails the test with a clear message when it's wrong.
 */
@SpringBootTest(properties = { "basera.seed=never", "basera.geocoding.enabled=false" })
@Import(TestMongo.class)
class BookingRulesTest {

    @Autowired TestData data;
    @Autowired BookingService bookingService;
    @Autowired ListingService listingService;
    @Autowired ReviewService reviewService;
    @Autowired BookingRepository bookings;
    @Autowired ReviewRepository reviews;

    User host;
    User guest;
    User otherGuest;
    Listing listing;

    @BeforeEach
    void setUp() {
        data.clear();
        host = data.user("host1", Role.HOST);
        guest = data.user("guest1", Role.CUSTOMER);
        otherGuest = data.user("guest2", Role.CUSTOMER);
        listing = data.listing(host, 4200, 4);
    }

    // ----- Price -----

    @Test
    void priceIsNightsTimesPricePlusTenPercentFee() {
        // The example in docs/API.md: 3 nights at 4200 -> 12600 + 1260 fee = 13860
        var price = BookingService.calculatePrice(4200, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 13));
        assertThat(price.nights()).isEqualTo(3);
        assertThat(price.subtotal()).isEqualTo(12600);
        assertThat(price.serviceFee()).isEqualTo(1260);
        assertThat(price.total()).isEqualTo(13860);
    }

    @Test
    void bookingStoresThePriceCalculatedOnTheServer() {
        Booking booking = book(guest, 10, 13, 2);
        assertThat(booking.getPricePerNight()).isEqualTo(4200);
        assertThat(booking.getTotalPrice()).isEqualTo(13860);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    // ----- Rules 1-3 -----

    @Test
    void checkInCannotBeInThePast() {
        assertRejected(() -> book(guest, -1, 2, 1), HttpStatus.BAD_REQUEST, "Check-in date can't be in the past.");
    }

    @Test
    void cannotBookMoreGuestsThanTheListingAllows() {
        assertRejected(() -> book(guest, 10, 12, 5), HttpStatus.BAD_REQUEST, "This place allows at most 4 guests.");
    }

    @Test
    void hostCannotBookTheirOwnListing() {
        assertRejected(() -> book(host, 10, 12, 1), HttpStatus.BAD_REQUEST, "You can't book your own listing.");
    }

    // ----- Rule 4: no overlaps -----

    @Test
    void overlappingDatesAreRejectedWith409() {
        book(guest, 10, 13, 1);
        assertRejected(() -> book(otherGuest, 12, 15, 1), HttpStatus.CONFLICT,
                "Sorry, those dates are already booked. Please pick different dates.");
        assertRejected(() -> book(otherGuest, 9, 14, 1), HttpStatus.CONFLICT, null); // surrounds the stay
    }

    @Test
    void checkOutDayCanBeTheNextGuestsCheckInDay() {
        book(guest, 10, 13, 1);
        assertThat(book(otherGuest, 13, 15, 1).getId()).isNotNull();
        assertThat(book(otherGuest, 8, 10, 1).getId()).isNotNull();
    }

    @Test
    void aCancelledBookingFreesItsDates() {
        Booking first = book(guest, 10, 13, 1);
        bookingService.cancel(first);
        assertThat(book(otherGuest, 10, 13, 1).getId()).isNotNull();
    }

    @Test
    void bookingsOnOtherListingsDontClash() {
        Listing second = data.listing(host, 1000, 2);
        book(guest, 10, 13, 1);
        BookingForm form = form(second.getId(), 10, 13, 1);
        assertThat(bookingService.create(form, TestData.login(otherGuest)).getId()).isNotNull();
    }

    // ----- Update & cancel -----

    @Test
    void changingDatesIgnoresTheBookingItself() {
        Booking booking = book(guest, 10, 13, 1);
        Booking changed = bookingService.update(booking, form(null, 11, 13, 2));
        assertThat(changed.getCheckIn()).isEqualTo(LocalDate.now().plusDays(11));
        assertThat(changed.getTotalPrice()).isEqualTo(9240); // 2 nights x 4200 + 10%
    }

    @Test
    void changingIntoAnotherBookingsDatesIsRejected() {
        Booking mine = book(guest, 10, 13, 1);
        book(otherGuest, 13, 16, 1);
        assertRejected(() -> bookingService.update(mine, form(null, 12, 14, 1)), HttpStatus.CONFLICT, null);
    }

    @Test
    void cancelledBookingsCantBeChangedOrCancelledAgain() {
        Booking booking = bookingService.cancel(book(guest, 10, 13, 1));
        assertRejected(() -> bookingService.update(booking, form(null, 10, 12, 1)), HttpStatus.BAD_REQUEST,
                "A cancelled booking can't be changed.");
        assertRejected(() -> bookingService.cancel(booking), HttpStatus.BAD_REQUEST, "This booking is already cancelled.");
    }

    @Test
    void onlyTheGuestAndTheHostCanSeeABooking() {
        Booking booking = book(guest, 10, 13, 1);
        assertThat(bookingService.requireAccess(booking.getId(), TestData.login(guest)).isGuest()).isTrue();
        assertThat(bookingService.requireAccess(booking.getId(), TestData.login(host)).isGuest()).isFalse();
        assertRejected(() -> bookingService.requireAccess(booking.getId(), TestData.login(otherGuest)),
                HttpStatus.FORBIDDEN, "You don't have access to this booking.");
        assertRejected(() -> bookingService.requireGuest(booking.getId(), TestData.login(host)),
                HttpStatus.FORBIDDEN, "Only the guest who made this booking can change it.");
    }

    // ----- Lists and search -----

    @Test
    void guestAndHostListsShowTheRightBookings() {
        book(guest, 10, 13, 1);
        book(otherGuest, 20, 22, 1);
        assertThat(bookingService.forGuest(guest.getId())).hasSize(1);
        assertThat(bookingService.forHost(host.getId())).hasSize(2);
        assertThat(bookingService.upcomingBookedRanges(listing)).hasSize(2);
    }

    @Test
    void searchHidesListingsBookedForTheChosenDates() {
        Listing free = data.listing(host, 1000, 2);
        book(guest, 10, 13, 1);
        var filters = ListingFilters.from(Map.of(
                "checkIn", LocalDate.now().plusDays(11).toString(),
                "checkOut", LocalDate.now().plusDays(12).toString()));
        assertThat(listingService.search(filters)).extracting(Listing::getId).containsExactly(free.getId());
    }

    @Test
    void deletingAListingDeletesItsReviewsAndBookings() {
        book(guest, 10, 13, 1);
        ReviewForm review = new ReviewForm();
        review.setRating(5);
        review.setComment("Great");
        reviewService.create(listing.getId(), TestData.login(guest), review);

        listingService.delete(listingService.getById(listing.getId()));

        assertThat(bookings.count()).isZero();
        assertThat(reviews.count()).isZero();
    }

    // ----- Helpers -----

    /** Books `listing` from today+startDay to today+endDay. */
    private Booking book(User who, int startDay, int endDay, int guests) {
        return bookingService.create(form(listing.getId(), startDay, endDay, guests), TestData.login(who));
    }

    private static BookingForm form(String listingId, int startDay, int endDay, int guests) {
        BookingForm form = new BookingForm();
        form.setListing(listingId);
        form.setCheckIn(LocalDate.now().plusDays(startDay));
        form.setCheckOut(LocalDate.now().plusDays(endDay));
        form.setGuests(guests);
        return form;
    }

    private static void assertRejected(Runnable action, HttpStatus status, String message) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(status);
                    if (message != null) {
                        assertThat(e.getMessage()).isEqualTo(message);
                    }
                });
    }
}
