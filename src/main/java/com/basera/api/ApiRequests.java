package com.basera.api;

import com.basera.dto.BookingForm;
import com.basera.dto.ListingForm;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * JSON request bodies. As in the Node API, listing and booking bodies are wrapped in the resource name:
 *   { "listing": { "title": ..., ... } }     { "booking": { "listing": ..., "checkIn": ..., ... } }
 * The inner objects are the same DTOs the website forms use, so the validation rules are shared.
 * @Valid on the inner field makes Spring check the rules inside it too.
 */
public final class ApiRequests {

    private ApiRequests() {
    }

    public record ListingRequest(@Valid @NotNull(message = "listing is required") ListingForm listing) {
    }

    public record BookingRequest(@Valid @NotNull(message = "booking is required") BookingForm booking) {
    }

    /** POST /api/auth/login - no role here, like the Node API. */
    public record LoginRequest(String username, String password) {
    }
}
