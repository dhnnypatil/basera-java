package com.basera.repository;

import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.basera.model.Booking;

/** Database access for bookings. */
public interface BookingRepository extends MongoRepository<Booking, String> {

    /**
     * Booking.findOverlapping from models/booking.js: is there a confirmed booking on this listing that
     * overlaps the requested stay?
     *
     * Two stays overlap when one starts before the other ends. The check-out day can be someone else's
     * check-in day, so the comparisons are strict ($lt / $gt). `ignoreBookingId` skips the booking being
     * edited (when it's null, `_id != null` matches every booking, so nothing is skipped).
     *
     * @Query holds the MongoDB filter as JSON; ?0, ?1, ... are the method's parameters in order.
     * `exists = true` turns "find" into "is there at least one?".
     */
    @Query(value = "{ 'listing': ?0, 'status': 'CONFIRMED', 'checkIn': { '$lt': ?2 }, 'checkOut': { '$gt': ?1 }, '_id': { '$ne': ?3 } }",
            exists = true)
    boolean existsOverlapping(ObjectId listingId, LocalDate checkIn, LocalDate checkOut, ObjectId ignoreBookingId);
}
