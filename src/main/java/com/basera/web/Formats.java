package com.basera.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Formatting helpers for templates (the format* functions in utils/helpers.js, which app.js put on app.locals).
 *
 * @Component("fmt") registers this class under the name "fmt", and Thymeleaf can call any Spring bean
 * with the @ prefix:
 *     EJS:        <%= formatPrice(listing.price) %>
 *     Thymeleaf:  th:text="${@fmt.price(listing.price)}"
 */
@Component("fmt")
public class Formats {

    private static final DateTimeFormatter DAY_MONTH_YEAR =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("en-IN"));

    /**
     * 2500 -> "₹2,500", 150000 -> "₹1,50,000".
     * Indian grouping puts commas after the first 3 digits from the right, then every 2. Java's number
     * formatter can only use one group size, so this is done by hand (Node's toLocaleString("en-IN") did it for us).
     */
    public String price(double amount) {
        long rupees = Math.round(amount);
        String digits = Long.toString(Math.abs(rupees));
        StringBuilder grouped = new StringBuilder();
        int length = digits.length();
        for (int i = 0; i < length; i++) {
            int fromRight = length - i;
            grouped.append(digits.charAt(i));
            boolean commaAfter = fromRight > 1 && (fromRight == 4 || (fromRight > 4 && fromRight % 2 == 0));
            if (commaAfter) {
                grouped.append(',');
            }
        }
        return (rupees < 0 ? "-₹" : "₹") + grouped;
    }

    /** LocalDate -> "16 Oct 2026" */
    public String date(LocalDate date) {
        return date == null ? "" : DAY_MONTH_YEAR.format(date);
    }

    /** Instant (createdAt) -> "16 Oct 2026", using the UTC calendar day like the Node app. */
    public String date(Instant instant) {
        return instant == null ? "" : DAY_MONTH_YEAR.format(instant.atZone(ZoneOffset.UTC));
    }

    /** Today as "2026-10-16", for <input type="date" min="..."> (todayISO). */
    public String todayIso() {
        return LocalDate.now().toString();
    }

    /** Date -> "2026-10-16" to pre-fill a date input (toDateInputValue). */
    public String dateInput(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    /** "1 night" / "3 nights", "1 guest" / "2 guests" - the `n === 1 ? ... : ...` bits in the EJS views. */
    public String plural(long count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    /** 4.666 -> "4.7" (toFixed(1)) */
    public String rating(Double rating) {
        return rating == null ? "" : String.format(Locale.ROOT, "%.1f", rating);
    }
}
