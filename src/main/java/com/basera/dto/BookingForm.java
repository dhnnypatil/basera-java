package com.basera.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Booking a stay, or changing one (bookingSchema in validators/schemas.js).
 * HTML field names: listing (hidden), checkIn, checkOut, guests. JSON: { "booking": { ... } }.
 */
public class BookingForm {

    /** Required when creating, ignored when updating (the booking already knows its listing). */
    @Pattern(regexp = "[0-9a-fA-F]{24}", message = "listing must be a valid id")
    private String listing;

    // @DateTimeFormat tells Spring how to read "2026-12-10" from a form field into a LocalDate.
    @NotNull(message = "checkIn is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @NotNull(message = "checkOut is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    @Min(value = 1, message = "guests must be greater than or equal to 1")
    @Max(value = 20, message = "guests must be less than or equal to 20")
    private Integer guests; // optional; empty means 1 (Joi's .default(1))

    /** Joi's checkOut: Joi.date().greater(Joi.ref("checkIn")), with the same custom message. */
    @JsonIgnore
    @AssertTrue(message = "Check-out must be after check-in")
    public boolean isCheckOutAfterCheckIn() {
        return checkIn == null || checkOut == null || checkOut.isAfter(checkIn);
    }

    public int guestsOrDefault() {
        return guests == null ? 1 : guests;
    }

    public String getListing() {
        return listing;
    }

    public void setListing(String listing) {
        this.listing = listing;
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

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }
}
