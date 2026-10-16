package com.basera.dto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The search filters from the query string: /listings?q=goa&category=Beach&minPrice=1000&sort=price_asc
 * (cleanFilters in services/listingService.js).
 *
 * from() keeps only known, non-empty values, drops numbers that aren't numbers, and drops the dates unless
 * both are valid and check-out is after check-in. Everything stays a String here because the page shows the
 * values back in the search form; the service parses the numbers.
 */
public class ListingFilters {

    /** FILTER_KEYS, in the order they appear in URLs. */
    public static final List<String> KEYS = List.of("q", "category", "minPrice", "maxPrice", "guests", "checkIn", "checkOut", "sort");

    private static final List<String> NUMBER_KEYS = List.of("minPrice", "maxPrice", "guests");

    private final Map<String, String> values;

    private ListingFilters(Map<String, String> values) {
        this.values = values;
    }

    public static ListingFilters from(Map<String, String> raw) {
        Map<String, String> clean = new LinkedHashMap<>(); // keeps the KEYS order
        for (String key : KEYS) {
            String value = raw.get(key);
            if (value != null && !value.isBlank()) {
                clean.put(key, value.trim());
            }
        }
        for (String key : NUMBER_KEYS) {
            if (clean.containsKey(key) && parseNumber(clean.get(key)) == null) {
                clean.remove(key);
            }
        }
        LocalDate checkIn = parseDate(clean.get("checkIn"));
        LocalDate checkOut = parseDate(clean.get("checkOut"));
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            clean.remove("checkIn");
            clean.remove("checkOut");
        }
        return new ListingFilters(clean);
    }

    public static ListingFilters none() {
        return new ListingFilters(new LinkedHashMap<>());
    }

    /** Only the sort order, e.g. for the home page's "top rated" list. */
    public static ListingFilters sortedBy(String sort) {
        return from(Map.of("sort", sort));
    }

    // ----- Read the filters -----

    public String get(String key) {
        return values.get(key);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** All active filters, e.g. to repeat them as hidden inputs in the "Sort by" form. */
    public Map<String, String> getParams() {
        return values;
    }

    public String getQ() {
        return values.get("q");
    }

    public String getCategory() {
        return values.get("category");
    }

    public String getSort() {
        return values.get("sort");
    }

    public Double getMinPrice() {
        return parseNumber(values.get("minPrice"));
    }

    public Double getMaxPrice() {
        return parseNumber(values.get("maxPrice"));
    }

    public Double getGuests() {
        return parseNumber(values.get("guests"));
    }

    public LocalDate getCheckIn() {
        return parseDate(values.get("checkIn"));
    }

    public LocalDate getCheckOut() {
        return parseDate(values.get("checkOut"));
    }

    /** Nights between the chosen dates, or 0 when no dates were picked (shown as a stay total on each card). */
    public long getNights() {
        LocalDate in = getCheckIn();
        LocalDate out = getCheckOut();
        return in == null || out == null ? 0 : ChronoUnit.DAYS.between(in, out);
    }

    private static Double parseNumber(String value) {
        if (value == null) {
            return null;
        }
        try {
            double number = Double.parseDouble(value);
            return Double.isFinite(number) ? number : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value); // "2026-12-10"
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
