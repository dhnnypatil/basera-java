package com.basera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * What the signup form sends (signupSchema in validators/schemas.js).
 *
 * A DTO ("data transfer object") holds exactly the fields we accept from the outside world. Spring copies
 * the form fields with matching names into it, then checks the annotations below when the controller
 * parameter is marked @Valid. Fields that aren't listed here are simply ignored, which is Joi's
 * stripUnknown: nobody can sneak in extra fields.
 * Phase 6 reuses this class for the JSON API's POST /api/auth/signup, as the Node app shared signupSchema.
 *
 * Joi rule                               ->  annotation
 *   Joi.string().required()              ->  @NotBlank / @NotEmpty / @NotNull
 *   .alphanum()                          ->  @Pattern(regexp = "[A-Za-z0-9]*")
 *   .min(3) / .max(30)                   ->  @Size(min = 3) / @Size(max = 30)
 *   .email()                             ->  @Email
 *   .valid("host", "customer")           ->  @Pattern(regexp = "host|customer")
 * The messages are the ones Joi produced, so users see the same text as on the Node site.
 */
public class SignupForm {

    @NotBlank(message = "username is not allowed to be empty")
    @Pattern(regexp = "[A-Za-z0-9]*", message = "username must only contain alpha-numeric characters")
    @Size(min = 3, message = "username length must be at least 3 characters long")
    @Size(max = 30, message = "username length must be less than or equal to 30 characters long")
    private String username;

    // Jakarta's @Email accepts "a@b"; the extra regexp also asks for a dot in the domain, like Joi does.
    @NotBlank(message = "email is not allowed to be empty")
    @Email(regexp = ".+@.+\\..+", message = "email must be a valid email")
    private String email;

    // Joi allowed up to 100 characters, but BCrypt only uses the first 72 bytes and Spring refuses longer
    // passwords, so the limit is 72 here.
    @NotEmpty(message = "password is not allowed to be empty")
    @Size(min = 6, message = "password length must be at least 6 characters long")
    @Size(max = 72, message = "password length must be less than or equal to 72 characters long")
    private String password;

    // A String rather than the Role enum, so a bad value gets Joi's message instead of a conversion error.
    @NotNull(message = "role is required")
    @Pattern(regexp = "host|customer", message = "role must be one of [host, customer]")
    private String role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
