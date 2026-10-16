package com.basera.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.RememberMeAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.savedrequest.NullRequestCache;

import com.basera.model.Role;
import com.basera.security.ApiAuthErrors;
import com.basera.security.AuthRedirects;
import com.basera.security.BaseraUserDetailsService;
import com.basera.security.LoginDetails;
import com.basera.security.RoleCheckingAuthenticationProvider;

/**
 * Security setup: config/passport.js, the session part of app.js, and the URL rules from middleware/auth.js.
 *
 * Spring Security is a chain of filters that runs before every controller (like app.use middleware).
 * There are two chains, because the website and the JSON API answer problems differently:
 *
 *   apiSecurity (/api/...)   401/403 as JSON, no CSRF tokens, login via POST /api/auth/login (api/AuthApiController)
 *   webSecurity (the rest)   flash + redirect, CSRF tokens in forms, login via the form at /login
 *
 * Both share the same logins: the session cookie and the remember-me cookie work for either.
 *
 * Differences from the Node app:
 *   - Access rules are listed here in one place, instead of isLoggedIn/isHost/isCustomer on each route.
 *     Ownership checks (isListingOwner, isReviewAuthor, ...) need the database and live in the services.
 *   - Website forms carry a CSRF token. Thymeleaf adds it automatically to any
 *     <form th:action="..." method="post">, so always use th:action, not plain action.
 *   - Logins survive restarts through a "remember-me" cookie (see rememberMeServices below) instead of
 *     sessions stored in MongoDB.
 *
 * A @Bean method's return value becomes a shared object that Spring hands to any class asking for that type.
 */
@Configuration
public class SecurityConfig {

    /**
     * The JSON API. @Order(1) + securityMatcher: requests to /api/... use this chain; everything else falls
     * through to webSecurity below. Rules copied from routes/api/*.js.
     *
     * No CSRF tokens here (an API client has no HTML form to put one in), as in the Node API. Other websites
     * still can't use a visitor's login: the cookies are SameSite=Lax, so browsers don't send them with
     * cross-site POST/PUT/DELETE requests, and the API only accepts JSON bodies.
     */
    @Bean
    @Order(1)
    SecurityFilterChain apiSecurity(HttpSecurity http, AuthenticationManager authenticationManager,
            TokenBasedRememberMeServices rememberMeServices, ApiAuthErrors apiErrors,
            @Value("${basera.remember-me-key}") String rememberMeKey) throws Exception {
        http
            .securityMatcher("/api/**")
            .authenticationManager(authenticationManager)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.POST, "/api/auth/signup", "/api/auth/login").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/listings", "/api/listings/*").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/listings").hasRole("HOST")
                    .requestMatchers(HttpMethod.POST, "/api/bookings").hasRole("CUSTOMER")
                    // Everything else needs a login: /api/auth/me, logout, editing listings (owner checked in the
                    // service), all other booking URLs (guest/host checked in the service).
                    .anyRequest().authenticated())
            .csrf(AbstractHttpConfigurer::disable)
            .rememberMe(remember -> remember.rememberMeServices(rememberMeServices).key(rememberMeKey))
            .requestCache(cache -> cache.requestCache(new NullRequestCache())) // no "return to" page for the API
            .logout(AbstractHttpConfigurer::disable) // POST /api/auth/logout is a normal controller method
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint(apiErrors)
                    .accessDeniedHandler(apiErrors));
        return http.build();
    }

    /** The website. POST /login and POST /logout are handled by Spring Security itself, not by controllers. */
    @Bean
    @Order(2)
    SecurityFilterChain webSecurity(HttpSecurity http, AuthenticationManager authenticationManager,
            TokenBasedRememberMeServices rememberMeServices, AuthRedirects redirects,
            @Value("${basera.remember-me-key}") String rememberMeKey) throws Exception {
        http
            .authenticationManager(authenticationManager)

            // Who may open which URL. The first matching rule wins, so specific rules go before general ones.
            .authorizeHttpRequests(auth -> auth
                    // Static files, the error page, and the login/signup pages (GET shows the form, POST submits it)
                    .requestMatchers("/css/**", "/js/**", "/uploads/**", "/favicon.ico", "/error").permitAll()
                    .requestMatchers("/login", "/signup").permitAll()
                    .requestMatchers(HttpMethod.GET, "/").permitAll()

                    // isHost (routes/listings.js, routes/dashboard.js). /listings/new must come before /listings/*.
                    .requestMatchers(HttpMethod.GET, "/listings/new").hasRole("HOST")
                    .requestMatchers(HttpMethod.POST, "/listings").hasRole("HOST")
                    .requestMatchers("/dashboard").hasRole("HOST")

                    // isCustomer (routes/reviews.js, routes/bookings.js)
                    .requestMatchers(HttpMethod.POST, "/listings/*/reviews").hasRole("CUSTOMER")
                    .requestMatchers(HttpMethod.GET, "/bookings").hasRole("CUSTOMER")
                    .requestMatchers(HttpMethod.POST, "/bookings").hasRole("CUSTOMER")

                    // Browsing listings is public
                    .requestMatchers(HttpMethod.GET, "/listings", "/listings/*").permitAll()

                    // Everything else needs a login (isLoggedIn): editing/deleting listings, deleting reviews,
                    // changing/cancelling bookings. Locking unknown URLs by default means a route we forget to
                    // list here is protected, not public.
                    .anyRequest().authenticated())

            // POST /login is handled by Spring Security. GET /login shows our page (web/UserController).
            .formLogin(form -> form
                    .loginPage("/login")
                    // Also read the role tab, for RoleCheckingAuthenticationProvider
                    .authenticationDetailsSource(request -> new LoginDetails(Role.fromParam(request.getParameter("role"))))
                    .successHandler(redirects)
                    .failureHandler(redirects))

            .rememberMe(remember -> remember
                    .rememberMeServices(rememberMeServices)
                    .key(rememberMeKey))

            // POST /logout (only POST, so a link on another site can't log you out). Clears the session and the
            // remember-me cookie.
            .logout(logout -> logout.logoutSuccessHandler(redirects))

            // Not logged in -> AuthRedirects.commence. Wrong role -> AuthRedirects.handle.
            .exceptionHandling(errors -> errors
                    .authenticationEntryPoint(redirects)
                    .accessDeniedHandler(redirects));

        return http.build();
    }

    /**
     * How a login is checked (passport.use(new LocalStrategy(...))): username + password (+ role tab) from the
     * login form or the API, or the remember-me cookie. A bean, so api/AuthApiController can use it too.
     */
    @Bean
    AuthenticationManager authenticationManager(BaseraUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder, @Value("${basera.remember-me-key}") String rememberMeKey) {
        return new ProviderManager(
                new RoleCheckingAuthenticationProvider(userDetailsService, passwordEncoder),
                new RememberMeAuthenticationProvider(rememberMeKey));
    }

    /**
     * The "remember-me" cookie that keeps a user logged in for a week, like the Node app's 1-week session cookie.
     *
     * Sessions are kept in memory (Spring Session MongoDB doesn't support Spring Boot 4), so a restart, a deploy
     * or Render's free tier going to sleep forgets every session. This cookie holds the username, an expiry date
     * and a signature made with `basera.remember-me-key` (SESSION_SECRET in production). On the next visit
     * Spring checks the signature and logs the user back in. Changing the password makes old cookies invalid,
     * because the password hash is part of the signature.
     */
    @Bean
    TokenBasedRememberMeServices rememberMeServices(BaseraUserDetailsService userDetailsService,
            @Value("${basera.remember-me-key}") String rememberMeKey) {
        TokenBasedRememberMeServices services = new TokenBasedRememberMeServices(rememberMeKey, userDetailsService);
        services.setAlwaysRemember(true); // the Node app had no "remember me" checkbox: every login lasted a week
        services.setTokenValiditySeconds((int) Duration.ofDays(7).toSeconds());
        services.setCookieCustomizer(cookie -> cookie.setAttribute("SameSite", "Lax"));
        return services;
    }

    /** Hashes passwords with BCrypt - the job of passport-local-mongoose's User.register. Used by the seeder and signup. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
