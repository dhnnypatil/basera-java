package com.basera.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/** cleanFilters from services/listingService.js: only known, sensible filters survive. */
class ListingFiltersTest {

    @Test
    void keepsKnownNonEmptyValues() {
        ListingFilters filters = ListingFilters.from(Map.of("q", "  goa ", "category", "Beach", "sort", "price_asc", "owner", "x"));
        assertThat(filters.getParams()).containsExactly(
                Map.entry("q", "goa"), Map.entry("category", "Beach"), Map.entry("sort", "price_asc"));
    }

    @Test
    void dropsNumbersThatArentNumbers() {
        ListingFilters filters = ListingFilters.from(Map.of("minPrice", "abc", "maxPrice", "3000", "guests", ""));
        assertThat(filters.getMinPrice()).isNull();
        assertThat(filters.getMaxPrice()).isEqualTo(3000);
        assertThat(filters.getParams()).containsOnlyKeys("maxPrice");
    }

    @Test
    void keepsDatesOnlyWhenBothAreValidAndInOrder() {
        assertThat(ListingFilters.from(Map.of("checkIn", "2026-12-10", "checkOut", "2026-12-13")).getNights()).isEqualTo(3);
        assertThat(ListingFilters.from(Map.of("checkIn", "2026-12-13", "checkOut", "2026-12-10")).getParams()).isEmpty();
        assertThat(ListingFilters.from(Map.of("checkIn", "2026-12-10", "checkOut", "2026-12-10")).getParams()).isEmpty();
        assertThat(ListingFilters.from(Map.of("checkIn", "tomorrow", "checkOut", "2026-12-10")).getParams()).isEmpty();
        assertThat(ListingFilters.from(Map.of("checkIn", "2026-12-10")).getParams()).isEmpty();
    }
}
