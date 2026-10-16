package com.basera.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import com.basera.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs a user in from our own code, without the login form: passport's `req.login(user, callback)`.
 * Signup uses it so a new user is logged in straight away.
 *
 * The login form does these same steps for us inside Spring Security. Doing it by hand takes four steps.
 * Used by signup (website and API) and by the API's POST /api/auth/login.
 */
@Component
public class ManualLogin {

    private final RememberMeServices rememberMe;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public ManualLogin(RememberMeServices rememberMe) {
        this.rememberMe = rememberMe;
    }

    /** Logs in a user we just created (signup). */
    public void logIn(User user, HttpServletRequest request, HttpServletResponse response) {
        CurrentUser principal = new CurrentUser(user);
        logIn(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()), request, response);
    }

    /** Logs in a user whose password was just checked (POST /api/auth/login). */
    public void logIn(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        // 1. New session id, so an id someone planted before login is useless afterwards
        //    (passport regenerates the session in req.login for the same reason).
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        // 2. Set the remember-me cookie. It's signed using the password hash (loaded again from the database
        //    if it was already erased), then the hash is removed from the session copy.
        rememberMe.loginSuccess(request, response, authentication);
        if (authentication.getPrincipal() instanceof CurrentUser principal) {
            principal.eraseCredentials();
        }

        // 3. Mark this request as logged in, and 4. save that in the session for the next requests.
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
