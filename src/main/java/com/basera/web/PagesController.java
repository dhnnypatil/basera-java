package com.basera.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.basera.dto.ListingFilters;
import com.basera.service.ListingService;

/**
 * The landing page (routes/pages.js + controllers/pages.js in one class).
 *
 * In Spring the route and its handler live together: @GetMapping("/") on a method is
 * `router.get("/", handler)`. The method fills a Model (the object passed to res.render) and returns the
 * template name - "pages/home" renders src/main/resources/templates/pages/home.html.
 */
@Controller
public class PagesController {

    private final ListingService listingService;

    public PagesController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredListings", listingService.featured(8));
        model.addAttribute("categoryLinks", CategoryBar.links(ListingFilters.none()));
        return "pages/home";
    }
}
