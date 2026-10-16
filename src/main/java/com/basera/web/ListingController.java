package com.basera.web;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.basera.dto.ListingFilters;
import com.basera.dto.ListingForm;
import com.basera.model.Listing;
import com.basera.security.CurrentUser;
import com.basera.service.BookingService;
import com.basera.service.ImageStorage;
import com.basera.service.ListingService;

import jakarta.validation.Valid;

/**
 * Listing pages for the WEBSITE (routes/listings.js + controllers/listings.js). RESTful URLs, as in Node:
 *
 *   Method   Path                  Who                 Method here     CRUD
 *   GET      /listings             anyone              index           Read all
 *   GET      /listings/new         host                newForm         (form)
 *   POST     /listings             host                create          Create
 *   GET      /listings/{id}        anyone              show            Read one
 *   GET      /listings/{id}/edit   owner               editForm        (form)
 *   PUT      /listings/{id}        owner               update          Update
 *   DELETE   /listings/{id}        owner               delete          Delete
 *
 * "Who" is enforced in two places: logged-in + role in config/SecurityConfig, ownership in
 * ListingService.requireOwned. @RequestMapping("/listings") on the class is router.use("/listings", ...).
 * {id} in a path is :id in Express; @PathVariable reads it (req.params.id).
 */
@Controller
@RequestMapping("/listings")
public class ListingController {

    private final ListingService listingService;
    private final BookingService bookingService;
    private final ImageStorage imageStorage;

    public ListingController(ListingService listingService, BookingService bookingService, ImageStorage imageStorage) {
        this.listingService = listingService;
        this.bookingService = bookingService;
        this.imageStorage = imageStorage;
    }

    /** READ all, with search & filters from the query string. @RequestParam Map = all of req.query. */
    @GetMapping
    public String index(@RequestParam Map<String, String> query, Model model) {
        ListingFilters filters = ListingFilters.from(query);
        model.addAttribute("listings", listingService.search(filters));
        model.addAttribute("filters", filters);
        model.addAttribute("nights", filters.getNights()); // if dates were picked, cards show the stay total
        model.addAttribute("categoryLinks", CategoryBar.links(filters));
        return "listings/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("listingForm", new ListingForm());
        return "listings/new";
    }

    /**
     * CREATE. `image` is the uploaded file (multer's req.file); it's empty when no file was chosen.
     * When the form has errors we show it again with the message, keeping what was typed
     * (the photo has to be picked again - browsers never re-fill file inputs).
     */
    @PostMapping
    public String create(@Valid @ModelAttribute("listingForm") ListingForm form, BindingResult errors,
            @RequestParam(required = false) MultipartFile image, @AuthenticationPrincipal CurrentUser user,
            Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("error", FormErrors.message(errors));
            return "listings/new";
        }
        Listing listing = listingService.create(form, user, imageStorage.store(image));
        redirect.addFlashAttribute("success", "Your place is live! 🎉");
        redirect.addAttribute("newId", listing.getId()); // fills {newId} below
        return "redirect:/listings/{newId}";
    }

    /** READ one. */
    @GetMapping("/{id}")
    public String show(@PathVariable String id, @AuthenticationPrincipal CurrentUser user, Model model) {
        Listing listing = listingService.getById(id);
        model.addAttribute("listing", listing);
        model.addAttribute("bookedRanges", bookingService.upcomingBookedRanges(listing));
        model.addAttribute("isOwner", user != null && listing.getOwner().getId().equals(user.getId()));
        model.addAttribute("isCustomer", user != null && user.isCustomer());
        model.addAttribute("alreadyReviewed", user != null && listing.getReviews().stream()
                .anyMatch(review -> review.getAuthor() != null && review.getAuthor().getId().equals(user.getId())));
        return "listings/show";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, @AuthenticationPrincipal CurrentUser user, Model model) {
        Listing listing = listingService.requireOwned(id, user);
        model.addAttribute("listing", listing);
        model.addAttribute("listingForm", ListingForm.from(listing));
        return "listings/edit";
    }

    /** UPDATE. "redirect:/listings/{id}" reuses the {id} from this request's URL (safely encoded). */
    @PutMapping("/{id}")
    public String update(@PathVariable String id, @Valid @ModelAttribute("listingForm") ListingForm form, BindingResult errors,
            @RequestParam(required = false) MultipartFile image, @AuthenticationPrincipal CurrentUser user,
            Model model, RedirectAttributes redirect) {
        Listing listing = listingService.requireOwned(id, user);
        if (errors.hasErrors()) {
            model.addAttribute("listing", listing);
            model.addAttribute("error", FormErrors.message(errors));
            return "listings/edit";
        }
        listingService.update(listing, form, imageStorage.store(image));
        redirect.addFlashAttribute("success", "Listing updated.");
        return "redirect:/listings/{id}";
    }

    /** DELETE - also deletes its reviews and bookings. */
    @DeleteMapping("/{id}")
    public String delete(@PathVariable String id, @AuthenticationPrincipal CurrentUser user, RedirectAttributes redirect) {
        listingService.delete(listingService.requireOwned(id, user));
        redirect.addFlashAttribute("success", "Listing deleted.");
        return "redirect:/dashboard";
    }
}
