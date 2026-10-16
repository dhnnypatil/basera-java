package com.basera.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.basera.api.ApiJson.UserJson;
import com.basera.api.ApiRequests.LoginRequest;
import com.basera.dto.SignupForm;
import com.basera.exception.AppException;
import com.basera.model.User;
import com.basera.security.CurrentUser;
import com.basera.security.ManualLogin;
import com.basera.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * REST API - Authentication (routes/api/auth.js + controllers/api/auth.js).
 * The API uses the same session cookie as the website: log in once, and the cookie is sent with every
 * following request (Postman / Thunder Client / curl -c / -b do this).
 *
 *   POST /api/auth/signup   -> create an account (and log in)   201 Created
 *   POST /api/auth/login    -> log in                           200 OK / 401 Unauthorized
 *   POST /api/auth/logout   -> log out                          204 No Content
 *   GET  /api/auth/me       -> who am I?                        200 OK
 *
 * @RestController = @Controller whose methods return data, which Spring sends as JSON (res.json(...)),
 * instead of a template name. @RequestBody reads the JSON body into an object (express.json()).
 * ResponseEntity lets a method pick the status code and headers: res.status(201).json(...).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final ManualLogin manualLogin;
    private final RememberMeServices rememberMe;

    public AuthApiController(AuthService authService, AuthenticationManager authenticationManager,
            ManualLogin manualLogin, RememberMeServices rememberMe) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.manualLogin = manualLogin;
        this.rememberMe = rememberMe;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, UserJson>> signup(@Valid @RequestBody SignupForm form,
            HttpServletRequest request, HttpServletResponse response) {
        User user = authService.registerUser(form);
        manualLogin.logIn(user, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("user", UserJson.from(user)));
    }

    @PostMapping("/login")
    public Map<String, UserJson> login(@RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            // The same check as the website's login form (without the role tab).
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        } catch (AuthenticationException e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }
        manualLogin.logIn(authentication, request, response);
        return Map.of("user", UserJson.from((CurrentUser) authentication.getPrincipal()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        // Forget the session and delete the remember-me cookie (what POST /logout does for the website).
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        ((LogoutHandler) rememberMe).logout(request, response, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Map<String, UserJson> me(@AuthenticationPrincipal CurrentUser user) {
        return Map.of("user", UserJson.from(user));
    }
}
