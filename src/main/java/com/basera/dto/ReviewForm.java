package com.basera.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The review form on a listing page (reviewSchema in validators/schemas.js).
 * HTML field names: rating, comment (the Node form used review[rating], review[comment]).
 */
public class ReviewForm {

    @NotNull(message = "rating is required")
    @Min(value = 1, message = "rating must be greater than or equal to 1")
    @Max(value = 5, message = "rating must be less than or equal to 5")
    private Integer rating;

    @NotBlank(message = "comment is not allowed to be empty")
    @Size(max = 1000, message = "comment length must be less than or equal to 1000 characters long")
    private String comment;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
