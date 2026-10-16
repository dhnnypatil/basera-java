package com.basera.web;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.basera.dto.BookingForm;
import com.basera.model.Booking;
import com.basera.security.CurrentUser;
import com.basera.service.BookingService;
import com.basera.service.BookingService.BookingAccess;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Bookings for the WEBSITE (routes/bookings.js + controllers/bookings.js). Every URL needs a login.
 *
 *   GET      /bookings             index      my trips (customer)
 *   POST     /bookings             create     book a place (customer)
 *   GET      /bookings/{id}/edit   editForm   change dates (the guest)
 *   PUT      /bookings/{id}        update     (the guest)
 *   DELETE   /bookings/{id}        cancel     (the guest OR the listing's host)
 */
@Controller
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** READ all of the logged-in customer's trips, split into upcoming and past/cancelled. */
    @GetMapping
    public String index(@AuthenticationPrincipal CurrentUser user, Model model) {
        List<Booking> bookings = bookingService.forGuest(user.getId());
        model.addAttribute("upcoming", bookings.stream().filter(Booking::isUpcoming).toList());
        model.addAttribute("past", bookings.stream().filter(b -> !b.isUpcoming()).toList());
        return "bookings/index";
    }

    /** CREATE. The listing id comes from a hidden field in the booking form. Errors go back to the listing page. */
    @PostMapping
    public String create(@Valid BookingForm form, BindingResult errors, @AuthenticationPrincipal CurrentUser user,
            HttpServletRequest request, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("error", FormErrors.message(errors));
            return "redirect:" + Flash.backUrl(request);
        }
        bookingService.create(form, user);
        redirect.addFlashAttribute("success", "Booking confirmed! Have a great stay.");
        return "redirect:/bookings";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, @AuthenticationPrincipal CurrentUser user, Model model) {
        model.addAttribute("booking", bookingService.requireGuest(id, user));
        return "bookings/edit";
    }

    /** UPDATE dates / guests. */
    @PutMapping("/{id}")
    public String update(@PathVariable String id, @Valid BookingForm form, BindingResult errors,
            @AuthenticationPrincipal CurrentUser user, RedirectAttributes redirect) {
        Booking booking = bookingService.requireGuest(id, user);
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("error", FormErrors.message(errors));
            return "redirect:/bookings/{id}/edit";
        }
        bookingService.update(booking, form);
        redirect.addFlashAttribute("success", "Booking updated.");
        return "redirect:/bookings";
    }

    /** DELETE (cancel). Guests go back to their trips, hosts to their dashboard. */
    @DeleteMapping("/{id}")
    public String cancel(@PathVariable String id, @AuthenticationPrincipal CurrentUser user, RedirectAttributes redirect) {
        BookingAccess access = bookingService.requireAccess(id, user);
        bookingService.cancel(access.booking());
        redirect.addFlashAttribute("success", "Booking cancelled.");
        return access.isGuest() ? "redirect:/bookings" : "redirect:/dashboard";
    }
}
