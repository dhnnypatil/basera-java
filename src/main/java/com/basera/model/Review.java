package com.basera.model;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

/**
 * A guest's star rating + comment on a listing (models/review.js).
 * Like in the Node app, a review doesn't know its listing - the listing keeps a list of its review ids.
 */
@Document("reviews")
public class Review {

    @Id
    private String id;

    private String comment;

    private int rating; // 1 to 5 (checked by the review form/DTO before we get here)

    /**
     * @DocumentReference = `{ type: ObjectId, ref: "User" }`.
     * The database stores only the user's _id; Spring Data loads the full User when it reads the review,
     * so it's like calling .populate("author") every time.
     */
    @DocumentReference
    private User author;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Review() {
    }

    public Review(String comment, int rating, User author) {
        this.comment = comment;
        this.rating = rating;
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
