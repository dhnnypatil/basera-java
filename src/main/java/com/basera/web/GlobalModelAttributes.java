package com.basera.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.basera.model.Category;
import com.basera.security.CurrentUser;
import com.basera.service.BookingService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Values every page can use - the Node app's res.locals middleware and app.locals:
 *
 *   res.locals.currUser = req.user || null;     ->  currUser()
 *   res.locals.currentPath = req.path;          ->  currentPath()
 *   app.locals.CATEGORIES                       ->  categories()
 *   app.locals.SERVICE_FEE_RATE                 ->  serviceFeeRate()
 *   formatPrice, formatDate, todayISO, ...      ->  the "fmt" bean (web/Formats), used as ${@fmt.price(...)}
 *
 * (success / error need no code here: flash messages from a redirect are added to the page's model automatically.)
 *
 * @ControllerAdvice applies this class to every controller. Each @ModelAttribute method runs before the
 * controller method, and its return value is added to the model under the given name.
 */
@ControllerAdvice(basePackages = "com.basera.web")
public class GlobalModelAttributes {

    /** The logged-in user, or null for visitors. @AuthenticationPrincipal asks Spring Security for it. */
    @ModelAttribute("currUser")
    public CurrentUser currUser(@AuthenticationPrincipal CurrentUser user) {
        return user;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("categories")
    public Category[] categories() {
        return Category.values();
    }

    @ModelAttribute("serviceFeeRate")
    public double serviceFeeRate() {
        return BookingService.SERVICE_FEE_RATE;
    }
}
