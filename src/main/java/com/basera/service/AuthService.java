package com.basera.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.basera.dto.SignupForm;
import com.basera.exception.AppException;
import com.basera.model.Role;
import com.basera.model.User;
import com.basera.repository.UserRepository;

/**
 * Account logic - signing up new users (services/authService.js).
 * Logging in is handled by Spring Security; see config/SecurityConfig.
 *
 * @Service marks a class that holds business logic. Spring creates one instance and passes it to every
 * class that asks for an AuthService in its constructor (the controllers).
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** CREATE - registers a user. The form has already been validated (SignupForm's annotations). */
    public User registerUser(SignupForm form) {
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT); // Mongoose's `lowercase: true, trim: true`

        if (users.existsByEmail(email)) {
            throw new AppException(HttpStatus.CONFLICT, "An account with that email already exists.");
        }
        if (users.existsByUsername(form.getUsername())) {
            throw new AppException(HttpStatus.CONFLICT, "That username is already taken.");
        }
        // SignupForm limits the length to 72 characters, but letters like "é" take 2 bytes. BCrypt's real limit is bytes.
        if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new AppException(HttpStatus.BAD_REQUEST, "password length must be less than or equal to 72 characters long");
        }

        User user = new User(form.getUsername(), email, passwordEncoder.encode(form.getPassword()),
                Role.fromParam(form.getRole()));
        try {
            return users.save(user);
        } catch (DuplicateKeyException e) {
            // Two people signing up with the same name at the same moment: the checks above both passed,
            // but the unique index in MongoDB rejects the second insert.
            String message = e.getMessage() != null && e.getMessage().contains("email")
                    ? "An account with that email already exists."
                    : "That username is already taken.";
            throw new AppException(HttpStatus.CONFLICT, message);
        }
    }
}
