package com.basera.model;

/**
 * A listing's photo: where to load it from, plus the file name so it can be deleted later
 * (the `image: { filename, url }` sub-object in models/listing.js).
 *
 * A record is a short way to write a small class that just holds values: Java generates the constructor,
 * the url() / filename() accessors, equals() and toString(). It's stored inside the listing document,
 * not in its own collection.
 */
public record Image(String url, String filename) {

    public static final String DEFAULT_URL =
            "https://images.unsplash.com/photo-1710609942195-b9dab8f48fc6?q=80&w=1200&auto=format&fit=crop";

    /** The placeholder photo, used when a listing is created without one. */
    public static Image placeholder() {
        return new Image(DEFAULT_URL, null);
    }
}
