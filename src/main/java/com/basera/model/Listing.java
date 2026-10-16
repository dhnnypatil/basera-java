package com.basera.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

/**
 * A place that can be booked (models/listing.js).
 *
 * Two things the Mongoose schema did are handled elsewhere now:
 *   - The cascade delete (deleting a listing deletes its reviews and bookings) moves into ListingService,
 *     because Spring Data has no "post findOneAndDelete" hook. It's also easier to find there.
 *   - Validation rules like `required` and `min: 0` move onto the form objects (DTOs) that receive user input,
 *     which is where Spring checks them.
 */
@Document("listings")
public class Listing {

    @Id
    private String id;

    private String title;

    private String description;

    private Image image = Image.placeholder();

    private int price; // price per night, in rupees

    private String location;

    private String country;

    private Category category = Category.ROOMS;

    private int maxGuests = 2;

    @DocumentReference
    private User owner;

    /** `reviews: [{ type: ObjectId, ref: "Review" }]` - stored as a list of ids, loaded as full Review objects. */
    @DocumentReference
    private List<Review> reviews = new ArrayList<>();

    /**
     * Map position. GeoJsonPoint is saved exactly like the Node app's geometry field:
     * { type: "Point", coordinates: [longitude, latitude] }. Null when geocoding failed - the page then hides the map.
     */
    private GeoJsonPoint geometry;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Listing() {
    }

    /**
     * Average star rating rounded to 2 decimals, or null when there are no reviews
     * (the `averageRating` virtual). A getter that isn't backed by a field acts like a Mongoose virtual:
     * templates can still write ${listing.averageRating}.
     */
    public Double getAverageRating() {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }
        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        return Math.round(average * 100) / 100.0;
    }

    public boolean isOwnedBy(User user) {
        return user != null && owner != null && owner.getId().equals(user.getId());
    }

    // ----- Getters and setters -----

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Image getImage() {
        return image;
    }

    /** No image or an empty URL -> the placeholder photo (the Mongoose `set:` on image.url). */
    public void setImage(Image image) {
        this.image = (image == null || image.url() == null || image.url().isBlank()) ? Image.placeholder() : image;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(int maxGuests) {
        this.maxGuests = maxGuests;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public GeoJsonPoint getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonPoint geometry) {
        this.geometry = geometry;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
