package com.basera.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.basera.dto.ReviewForm;
import com.basera.security.CurrentUser;
import com.basera.service.ReviewService;

import jakarta.validation.Valid;

/**
 * Reviews, nested under a listing (routes/reviews.js + controllers/reviews.js):
 *
 *   POST     /listings/{id}/reviews              create   (customers only - config/SecurityConfig)
 *   DELETE   /listings/{id}/reviews/{reviewId}   delete   (the review's author - ReviewService)
 */
@Controller
@RequestMapping("/listings/{id}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public String create(@PathVariable String id, @Valid ReviewForm form, BindingResult errors,
            @AuthenticationPrincipal CurrentUser user, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            redirect.addFlashAttribute("error", FormErrors.message(errors));
            return "redirect:/listings/{id}#reviews";
        }
        reviewService.create(id, user, form);
        redirect.addFlashAttribute("success", "Thanks for your review!");
        return "redirect:/listings/{id}#reviews";
    }

    @DeleteMapping("/{reviewId}")
    public String delete(@PathVariable String id, @PathVariable String reviewId,
            @AuthenticationPrincipal CurrentUser user, RedirectAttributes redirect) {
        reviewService.delete(id, reviewId, user);
        redirect.addFlashAttribute("success", "Review deleted.");
        return "redirect:/listings/{id}#reviews";
    }
}
