package com.basera.web;

import java.io.IOException;
import java.net.URI;

import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Flash messages for code that runs outside a controller (connect-flash's req.flash + res.redirect).
 *
 * Inside a controller, use RedirectAttributes instead:
 *     redirect.addFlashAttribute("success", "Listing created!");
 *     return "redirect:/listings";
 * But the login/logout handlers in security/AuthRedirects run before any controller, so they use this helper.
 * Both store the message in the session the same way, and the next page shows it as ${success} or ${error}
 * (templates/includes/flash.html), then it's gone.
 */
public final class Flash {

    private static final FlashMapManager FLASH_MAPS = new SessionFlashMapManager();

    private Flash() {
        // only static methods - nobody needs to create a Flash object
    }

    /** Sets a one-time message ("success" or "error") and redirects to `url`. */
    public static void redirect(HttpServletRequest request, HttpServletResponse response,
            String url, String type, String message) throws IOException {
        FlashMap flash = new FlashMap();
        flash.put(type, message);
        // Only show the message on the page we're redirecting to, not on some other request that happens first.
        flash.setTargetRequestPath(UriComponentsBuilder.fromUriString(url).build().getPath());
        FLASH_MAPS.saveOutputFlashMap(flash, request, response);
        response.sendRedirect(url);
    }

    /**
     * The page the user came from (Express's res.redirect("back")). Only the path is kept, so a Referer
     * header pointing at another website can't send people there.
     */
    public static String backUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return "/";
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
