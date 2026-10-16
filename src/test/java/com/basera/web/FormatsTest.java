package com.basera.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** The template helpers (formatPrice / formatDate in utils/helpers.js). Plain Java - no Spring, no database. */
class FormatsTest {

    private final Formats fmt = new Formats();

    @Test
    void pricesUseIndianGrouping() {
        assertThat(fmt.price(0)).isEqualTo("₹0");
        assertThat(fmt.price(999)).isEqualTo("₹999");
        assertThat(fmt.price(2500)).isEqualTo("₹2,500");
        assertThat(fmt.price(13860)).isEqualTo("₹13,860");
        assertThat(fmt.price(150000)).isEqualTo("₹1,50,000");
        assertThat(fmt.price(12345678)).isEqualTo("₹1,23,45,678");
        assertThat(fmt.price(2499.6)).isEqualTo("₹2,500"); // Math.round, like the Node helper
    }

    @Test
    void datesLookLikeTheNodeApp() {
        assertThat(fmt.date(LocalDate.of(2026, 10, 16))).isEqualTo("16 Oct 2026");
        assertThat(fmt.date(LocalDate.of(2026, 1, 5))).isEqualTo("5 Jan 2026");
        assertThat(fmt.dateInput(LocalDate.of(2026, 1, 5))).isEqualTo("2026-01-05");
    }

    @Test
    void pluralsAndRatings() {
        assertThat(fmt.plural(1, "night", "nights")).isEqualTo("1 night");
        assertThat(fmt.plural(3, "night", "nights")).isEqualTo("3 nights");
        assertThat(fmt.rating(4.666)).isEqualTo("4.7");
    }
}
