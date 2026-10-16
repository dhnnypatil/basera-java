package com.basera.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.util.UriComponentsBuilder;

import com.basera.dto.ListingFilters;
import com.basera.model.Category;

/**
 * The links in the horizontal category bar (views/includes/category-bar.ejs + the linkForCategory function
 * in views/listings/index.ejs). Each link keeps the current filters and only changes the category.
 * The URLs are built here because Thymeleaf templates are meant for markup, not for logic like this.
 */
public final class CategoryBar {

    /** One icon in the bar. */
    public record Link(String label, String icon, String url, boolean active) {
    }

    private CategoryBar() {
    }

    public static List<Link> links(ListingFilters filters) {
        Category active = Category.fromParam(filters.getCategory());
        List<Link> links = new ArrayList<>();
        links.add(new Link("All", "fa-solid fa-fire", urlFor(filters, null), active == null));
        for (Category category : Category.values()) {
            links.add(new Link(category.getLabel(), category.getIcon(), urlFor(filters, category), category == active));
        }
        return links;
    }

    private static String urlFor(ListingFilters filters, Category category) {
        UriComponentsBuilder url = UriComponentsBuilder.fromPath("/listings");
        filters.getParams().forEach((key, value) -> {
            if (!key.equals("category")) {
                url.queryParam(key, value);
            }
        });
        if (category != null) {
            url.queryParam("category", category.getLabel());
        }
        return url.encode().build().toUriString(); // "Iconic Cities" -> Iconic%20Cities
    }
}
