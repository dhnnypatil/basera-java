package com.basera.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.basera.model.Booking;
import com.basera.model.BookingStatus;
import com.basera.model.Listing;
import com.basera.security.CurrentUser;
import com.basera.service.BookingService;
import com.basera.service.ListingService;

/** GET /dashboard - the host's home: their listings, bookings received, and quick stats (controllers/dashboard.js). */
@Controller
public class DashboardController {

    /** The three numbers at the top of the dashboard. */
    public record Stats(int totalListings, long upcomingBookings, long totalEarnings) {
    }

    private final ListingService listingService;
    private final BookingService bookingService;

    public DashboardController(ListingService listingService, BookingService bookingService) {
        this.listingService = listingService;
        this.bookingService = bookingService;
    }

    @GetMapping("/dashboard")
    public String hostDashboard(@AuthenticationPrincipal CurrentUser user, Model model) {
        List<Listing> listings = listingService.byOwner(user.getId());
        List<Booking> bookings = bookingService.forHost(user.getId());

        List<Booking> confirmed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).toList();
        Stats stats = new Stats(
                listings.size(),
                confirmed.stream().filter(Booking::isUpcoming).count(),
                // Hosts earn nights x nightly price (the service fee goes to the platform).
                confirmed.stream().mapToLong(b -> b.getNights() * b.getPricePerNight()).sum());

        model.addAttribute("listings", listings);
        model.addAttribute("bookings", bookings);
        model.addAttribute("stats", stats);
        return "dashboard/index";
    }
}
