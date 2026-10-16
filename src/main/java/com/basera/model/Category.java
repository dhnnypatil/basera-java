package com.basera.model;

/**
 * Listing categories + the Font Awesome icon shown for each (CATEGORIES in the Node app's utils/constants.js).
 *
 * Java enums can carry data: each value below is created by calling the constructor with its label and icon.
 * The database stores the name (e.g. "ICONIC_CITIES"); pages show the label ("Iconic Cities").
 */
public enum Category {
    ROOMS("Rooms", "fa-solid fa-bed"),
    ICONIC_CITIES("Iconic Cities", "fa-solid fa-mountain-city"),
    MOUNTAINS("Mountains", "fa-solid fa-mountain"),
    BEACH("Beach", "fa-solid fa-umbrella-beach"),
    CASTLES("Castles", "fa-brands fa-fort-awesome"),
    CAMPING("Camping", "fa-solid fa-campground"),
    POOLS("Pools", "fa-solid fa-person-swimming"),
    FARMHOUSE("Farmhouse", "fa-solid fa-house-chimney-window"),
    ARCTIC("Arctic", "fa-regular fa-snowflake"),
    TREEHOUSE("Treehouse", "fa-solid fa-tree");

    private final String label;
    private final String icon;

    Category(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    /**
     * "Beach", "Iconic Cities" or "ICONIC_CITIES" -> the matching category, or null if there is none.
     * URLs and the JSON API use the label, like the Node app (?category=Iconic%20Cities).
     */
    public static Category fromParam(String value) {
        if (value == null) {
            return null;
        }
        for (Category category : values()) {
            if (category.label.equalsIgnoreCase(value.trim()) || category.name().equalsIgnoreCase(value.trim())) {
                return category;
            }
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }
}
