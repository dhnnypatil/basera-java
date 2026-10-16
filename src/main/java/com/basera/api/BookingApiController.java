package com.basera.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.basera.api.ApiJson.BookingJson;
import com.basera.api.ApiRequests.BookingRequest;
import com.basera.model.Booking;
import com.basera.security.CurrentUser;
import com.basera.service.BookingService;

import jakarta.validation.Valid;

/**
 * REST API - Bookings (routes/api/bookings.js + controllers/api/bookings.js). Every URL needs a login.
 *
 *   GET    /api/bookings        logged in       Read all   customer: my trips / host: bookings on my listings
 *   GET    /api/bookings/{id}   guest or host   Read one
 *   POST   /api/bookings        customer        Create     201 Created / 409 Conflict if the dates are taken
 *   PUT    /api/bookings/{id}   guest           Update     change dates / guests
 *   DELETE /api/bookings/{id}   guest or host   Delete     soft delete: status becomes "cancelled"
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingApiController {

    private final BookingService bookingService;

    public BookingApiController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public Map<String, Object> index(@AuthenticationPrincipal CurrentUser user) {
        List<Booking> bookings = user.isHost() ? bookingService.forHost(user.getId()) : bookingService.forGuest(user.getId());
        return Map.of("count", bookings.size(), "bookings", bookings.stream().map(BookingJson::from).toList());
    }

    @GetMapping("/{id}")
    public Map<String, BookingJson> show(@PathVariable String id, @AuthenticationPrincipal CurrentUser user) {
        return Map.of("booking", BookingJson.from(bookingService.requireAccess(id, user).booking()));
    }

    @PostMapping
    public ResponseEntity<Map<String, BookingJson>> create(@Valid @RequestBody BookingRequest body,
            @AuthenticationPrincipal CurrentUser user) {
        Booking booking = bookingService.create(body.booking(), user);
        return ResponseEntity.created(URI.create("/api/bookings/" + booking.getId()))
                .body(Map.of("booking", BookingJson.from(booking)));
    }

    @PutMapping("/{id}")
    public Map<String, BookingJson> update(@PathVariable String id, @Valid @RequestBody BookingRequest body,
            @AuthenticationPrincipal CurrentUser user) {
        Booking booking = bookingService.requireGuest(id, user);
        return Map.of("booking", BookingJson.from(bookingService.update(booking, body.booking())));
    }

    @DeleteMapping("/{id}")
    public Map<String, BookingJson> cancel(@PathVariable String id, @AuthenticationPrincipal CurrentUser user) {
        Booking booking = bookingService.requireAccess(id, user).booking();
        return Map.of("booking", BookingJson.from(bookingService.cancel(booking)));
    }
}
