package com.basera.service;

import org.springframework.stereotype.Service;

import com.basera.dto.ReviewForm;
import com.basera.exception.AppException;
import com.basera.model.Listing;
import com.basera.model.Review;
import com.basera.repository.ListingRepository;
import com.basera.repository.ReviewRepository;
import com.basera.repository.UserRepository;
import com.basera.security.CurrentUser;

/**
 * Review business logic (services/reviewService.js).
 * Reviews are stored in their own collection; the listing keeps a list of review ids.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final ListingRepository listings;
    private final UserRepository users;

    public ReviewService(ReviewRepository reviews, ListingRepository listings, UserRepository users) {
        this.reviews = reviews;
        this.listings = listings;
        this.users = users;
    }

    /** CREATE - adds a review to a listing. */
    public Review create(String listingId, CurrentUser author, ReviewForm form) {
        Listing listing = listings.findById(listingId)
                .orElseThrow(() -> AppException.notFound("Listing not found.", "/listings"));
        if (listing.getOwner().getId().equals(author.getId())) {
            throw AppException.badRequest("You can't review your own listing.");
        }
        Review review = reviews.save(new Review(form.getComment().trim(), form.getRating(),
                users.findById(author.getId()).orElseThrow()));
        listing.getReviews().add(review); // listing.reviews.push(review._id)
        listings.save(listing);
        return review;
    }

    /**
     * DELETE - only the review's author may delete it (isReviewAuthor in middleware/auth.js).
     * Removes the review and its id from the listing's reviews list ($pull).
     */
    public void delete(String listingId, String reviewId, CurrentUser user) {
        String listingPage = "/listings/" + listingId;
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> AppException.notFound("Review not found.", listingPage));
        if (review.getAuthor() == null || !review.getAuthor().getId().equals(user.getId())) {
            throw AppException.forbidden("You are not the author of this review.", listingPage);
        }
        listings.findById(listingId).ifPresent(listing -> {
            listing.getReviews().removeIf(r -> r == null || r.getId().equals(reviewId));
            listings.save(listing);
        });
        reviews.delete(review);
    }
}
