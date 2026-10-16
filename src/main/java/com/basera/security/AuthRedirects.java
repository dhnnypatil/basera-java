package com.basera.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import com.basera.model.Role;
import com.basera.web.Flash;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * What the website shows after each step of logging in or out, and when someone isn't allowed in.
 * Every answer is "flash a message and redirect", like the Node app.
 *
 *   Method                     When                                  Node equivalent
 *   onAuthenticationSuccess    login worked                          login() in controllers/users.js
 *   onAuthenticationFailure    wrong password / wrong role tab       login() in controllers/users.js
 *   onLogoutSuccess            after POST /logout                    logout() in controllers/users.js
 *   commence                   not logged in                         isLoggedIn in middleware/auth.js
 *   handle                     logged in, but wrong role             isHost / isCustomer in middleware/auth.js
 *
 * Spring Security calls these through the interfaces this class implements; SecurityConfig plugs it in.
 * The JSON API (/api/...) gets 401/403 JSON answers from ApiAuthErrors instead.
 */
@Component
public class AuthRedirects implements AuthenticationSuccessHandler, AuthenticationFailureHandler,
        LogoutSuccessHandler, AuthenticationEntryPoint, AccessDeniedHandler {

    /** Where Spring Security saves the page someone wanted before it sent them to log in (req.session.returnTo). */
    private final RequestCache requestCache = new HttpSessionRequestCache();

    /** Where to send a user after they log in / sign up (homeFor in controllers/users.js). */
    public static String homeFor(Role role) {
        return role == Role.HOST ? "/dashboard" : "/listings";
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        CurrentUser user = (CurrentUser) authentication.getPrincipal();

        // Send them back to the page they were trying to open, or to their home page.
        SavedRequest returnTo = requestCache.getRequest(request, response);
        requestCache.removeRequest(request, response);
        String target = returnTo != null ? returnTo.getRedirectUrl() : homeFor(user.getRole());

        Flash.redirect(request, response, target, "success", "Welcome back, " + user.getUsername() + "!");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        if (exception instanceof WrongRoleException wrongRole) {
            Role actual = wrongRole.getActualRole();
            Flash.redirect(request, response, "/login?role=" + actual.getParam(), "error",
                    "This is a " + actual.getLabel() + " account - please use the " + actual.getLabel() + " tab to log in.");
            return;
        }
        Role tab = Role.fromParam(request.getParameter("role"));
        Flash.redirect(request, response, "/login?role=" + tab.getParam(), "error", "Invalid username or password.");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        Flash.redirect(request, response, "/", "success", "You are logged out.");
    }

    /** Not logged in. Spring Security has already saved the page they wanted (GET requests only, like Node). */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        Flash.redirect(request, response, "/login", "error", "Please log in first.");
    }

    /** Logged in, but not allowed. Also called when a form's CSRF token is missing or out of date. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        if (accessDeniedException instanceof CsrfException) {
            // Usually the form was opened before the server restarted, so its hidden token is no longer valid.
            Flash.redirect(request, response, Flash.backUrl(request), "error", "Your session expired - please try again.");
            return;
        }

        String message = wrongRoleMessage(SecurityContextHolder.getContext().getAuthentication());
        Flash.redirect(request, response, "/listings", "error", message);
    }

    /**
     * Every role rule in SecurityConfig allows exactly one role, so a customer was refused a host-only page
     * and a host was refused a customer-only page. (Also used by ApiAuthErrors for the JSON API.)
     */
    static String wrongRoleMessage(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();
        return principal instanceof CurrentUser user && user.isCustomer()
                ? "Only hosts can do that. Log in with a Host account."
                : "Only customers can do that. Log in with a Customer account.";
    }
}
