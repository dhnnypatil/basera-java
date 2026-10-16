package com.basera.dto;

import org.hibernate.validator.constraints.URL;

import com.basera.model.Category;
import com.basera.model.Listing;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What the listing form (and the JSON API) sends to create or edit a listing (listingSchema in validators/schemas.js).
 * Fields that aren't here - like `owner` or `reviews` - can't be set from outside (Joi's stripUnknown).
 *
 * Field names in the HTML form: title, description, location, country, price, category, maxGuests,
 * imageInput.url (the "paste an image URL" box) and the file input `image`, which the controller reads separately.
 * In JSON (phase 6) the body is { "listing": { ..., "image": { "url": "..." } } }, as in docs/API.md.
 */
public class ListingForm {

    @NotBlank(message = "title is not allowed to be empty")
    @Size(max = 100, message = "title length must be less than or equal to 100 characters long")
    private String title;

    @Size(max = 2000, message = "description length must be less than or equal to 2000 characters long")
    private String description;

    @NotBlank(message = "location is not allowed to be empty")
    private String location;

    @NotBlank(message = "country is not allowed to be empty")
    private String country;

    // Integer (not int) so "no value" is null and gets the "is required" message instead of becoming 0.
    @NotNull(message = "price is required")
    @Min(value = 0, message = "price must be greater than or equal to 0")
    private Integer price;

    @NotBlank(message = "category is required")
    private String category;

    @Min(value = 1, message = "maxGuests must be greater than or equal to 1")
    @Max(value = 20, message = "maxGuests must be less than or equal to 20")
    private Integer maxGuests; // optional; empty means 2 (Joi's .default(2))

    /** @Valid checks the annotations inside ImageInput too. @JsonProperty: it's called "image" in JSON. */
    @Valid
    @JsonProperty("image")
    private ImageInput imageInput;

    /** The "...or paste an image URL" box. An empty URL is allowed (it means "keep / use the default photo"). */
    public static class ImageInput {

        @URL(message = "image.url must be a valid uri")
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * Joi's .valid(...CATEGORY_NAMES). An @AssertTrue method is a custom rule: validation fails when it returns false.
     * (category is a String rather than the Category enum so a bad value gets this message, not a conversion error.)
     */
    @JsonIgnore
    @AssertTrue(message = "category must be one of [Rooms, Iconic Cities, Mountains, Beach, Castles, Camping, Pools, Farmhouse, Arctic, Treehouse]")
    public boolean isCategoryValid() {
        return category == null || category.isBlank() || Category.fromParam(category) != null;
    }

    /** A form pre-filled from an existing listing, for the edit page. */
    public static ListingForm from(Listing listing) {
        ListingForm form = new ListingForm();
        form.title = listing.getTitle();
        form.description = listing.getDescription();
        form.location = listing.getLocation();
        form.country = listing.getCountry();
        form.price = listing.getPrice();
        form.category = listing.getCategory().getLabel();
        form.maxGuests = listing.getMaxGuests();
        return form;
    }

    /** The pasted image URL, or null when the box was left empty. */
    @JsonIgnore
    public String getPastedImageUrl() {
        return imageInput == null || imageInput.getUrl() == null || imageInput.getUrl().isBlank()
                ? null
                : imageInput.getUrl().trim();
    }

    // ----- Getters and setters -----

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

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public ImageInput getImageInput() {
        return imageInput;
    }

    public void setImageInput(ImageInput imageInput) {
        this.imageInput = imageInput;
    }
}
