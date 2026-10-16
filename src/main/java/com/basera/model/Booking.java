package com.basera.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

/**
 * A customer's stay at a listing (models/booking.js).
 *
 * Dates are LocalDate - a calendar date with no time or timezone ("2026-10-16"), which is exactly what a
 * check-in day is. The Node app had to fake this with "midnight UTC" Date objects and careful maths.
 *
 * The overlap query (`Booking.findOverlapping`) lives in BookingRepository.
 */
@Document("bookings")
public class Booking {

    @Id
    private String id;

    @DocumentReference
    private Listing listing;

    @DocumentReference
    private User guest; // the customer who made the booking

    private LocalDate checkIn;

    private LocalDate checkOut;

    private int guests = 1;

    /** Copied from the listing at booking time, so later price changes don't affect old bookings. */
    private int pricePerNight;

    private int totalPrice;

    private BookingStatus status = BookingStatus.CONFIRMED;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Booking() {
    }

    // ----- The two virtuals from booking.js -----

    public long getNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public boolean isUpcoming() {
        return status == BookingStatus.CONFIRMED && checkOut.isAfter(LocalDate.now());
    }

    /**
     * The status badge text: a confirmed stay whose check-out has passed shows as "completed"
     * (displayStatus in views/bookings/index.ejs and views/dashboard/index.ejs).
     */
    public String getDisplayStatus() {
        return status == BookingStatus.CONFIRMED && !isUpcoming() ? "completed" : status.name().toLowerCase();
    }

    // ----- Getters and setters -----

    public String getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public User getGuest() {
        return guest;
    }

    public void setGuest(User guest) {
        this.guest = guest;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public void setGuests(int guests) {
        this.guests = guests;
    }

    public int getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(int pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
