package com.basera.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.basera.api.ApiJson.ListingJson;
import com.basera.api.ApiRequests.ListingRequest;
import com.basera.dto.ListingFilters;
import com.basera.dto.ListingForm;
import com.basera.model.Listing;
import com.basera.security.CurrentUser;
import com.basera.service.ImageStorage;
import com.basera.service.ListingService;

import jakarta.validation.Valid;

/**
 * REST API - Listings (routes/api/listings.js + controllers/api/listings.js).
 * Every method responds with JSON and a meaningful status code.
 *
 *   GET    /api/listings        anyone   Read all    200  (same ?q=&category=&... filters as the website)
 *   GET    /api/listings/{id}   anyone   Read one    200 / 404
 *   POST   /api/listings        host     Create      201 + Location header
 *   PUT    /api/listings/{id}   owner    Update      200
 *   DELETE /api/listings/{id}   owner    Delete      204
 *
 * POST and PUT accept JSON ({ "listing": {...} }), or multipart/form-data with the same field names as the
 * website form (title, price, ...) plus the photo in a field called `image`. `consumes` picks the method.
 */
@RestController
@RequestMapping("/api/listings")
public class ListingApiController {

    private final ListingService listingService;
    private final ImageStorage imageStorage;

    public ListingApiController(ListingService listingService, ImageStorage imageStorage) {
        this.listingService = listingService;
        this.imageStorage = imageStorage;
    }

    @GetMapping
    public Map<String, Object> index(@RequestParam Map<String, String> query) {
        List<ListingJson> listings = listingService.search(ListingFilters.from(query)).stream().map(ListingJson::from).toList();
        return Map.of("count", listings.size(), "listings", listings);
    }

    @GetMapping("/{id}")
    public Map<String, ListingJson> show(@PathVariable String id) {
        return Map.of("listing", ListingJson.from(listingService.getById(id)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, ListingJson>> create(@Valid @RequestBody ListingRequest body,
            @AuthenticationPrincipal CurrentUser user) {
        return created(listingService.create(body.listing(), user, null));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, ListingJson>> createWithPhoto(@Valid @ModelAttribute ListingForm form,
            @RequestParam(required = false) MultipartFile image, @AuthenticationPrincipal CurrentUser user) {
        return created(listingService.create(form, user, imageStorage.store(image)));
    }

    /** PUT replaces the editable fields - send all of them (as in the Node API). */
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, ListingJson> update(@PathVariable String id, @Valid @RequestBody ListingRequest body,
            @AuthenticationPrincipal CurrentUser user) {
        Listing listing = listingService.requireOwned(id, user);
        return Map.of("listing", ListingJson.from(listingService.update(listing, body.listing(), null)));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, ListingJson> updateWithPhoto(@PathVariable String id, @Valid @ModelAttribute ListingForm form,
            @RequestParam(required = false) MultipartFile image, @AuthenticationPrincipal CurrentUser user) {
        Listing listing = listingService.requireOwned(id, user);
        return Map.of("listing", ListingJson.from(listingService.update(listing, form, imageStorage.store(image))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> destroy(@PathVariable String id, @AuthenticationPrincipal CurrentUser user) {
        listingService.delete(listingService.requireOwned(id, user));
        return ResponseEntity.noContent().build();
    }

    /** 201 Created, with a Location header pointing at the new listing. */
    private static ResponseEntity<Map<String, ListingJson>> created(Listing listing) {
        return ResponseEntity.created(URI.create("/api/listings/" + listing.getId()))
                .body(Map.of("listing", ListingJson.from(listing)));
    }
}
