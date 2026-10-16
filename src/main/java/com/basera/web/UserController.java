package com.basera.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.basera.dto.SignupForm;
import com.basera.exception.AppException;
import com.basera.model.Role;
import com.basera.model.User;
import com.basera.security.AuthRedirects;
import com.basera.security.ManualLogin;
import com.basera.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Signup and the login page for the WEBSITE (routes/users.js + controllers/users.js).
 *
 *   GET  /signup   -> signupForm
 *   POST /signup   -> signup
 *   GET  /login    -> loginForm
 *   POST /login    -> handled by Spring Security (config/SecurityConfig, security/AuthRedirects)
 *   POST /logout   -> handled by Spring Security
 *
 * Like the Node app, the forms have two tabs, Customer and Host, sent as `role`. ?role=host picks the Host tab.
 */
@Controller
public class UserController {

    private final AuthService authService;
    private final ManualLogin manualLogin;

    public UserController(AuthService authService, ManualLogin manualLogin) {
        this.authService = authService;
        this.manualLogin = manualLogin;
    }

    // @RequestParam reads ?role=... from the URL (req.query.role); required = false because it's optional.
    @GetMapping("/signup")
    public String signupForm(@RequestParam(required = false) String role, Model model) {
        SignupForm form = new SignupForm();
        form.setRole(Role.fromParam(role).getParam());
        model.addAttribute("signupForm", form);
        return "users/signup";
    }

    /**
     * CREATE a user, then log them straight in.
     *
     * @Valid checks SignupForm's annotations (validate(signupSchema) in routes/users.js), and the result lands
     * in `errors` instead of stopping the request. One difference from Node: when something is wrong we show
     * the form again with the message, instead of redirecting back, so the typed username and email stay filled in.
     */
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm form, BindingResult errors, Model model,
            HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            model.addAttribute("error", FormErrors.message(errors));
            return "users/signup";
        }

        User user;
        try {
            user = authService.registerUser(form);
        } catch (AppException e) { // username or email already taken
            model.addAttribute("error", e.getMessage());
            return "users/signup";
        }

        manualLogin.logIn(user, request, response);
        redirect.addFlashAttribute("success", "Welcome to Basera, " + user.getUsername() + "!");
        return "redirect:" + AuthRedirects.homeFor(user.getRole());
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String role, Model model) {
        model.addAttribute("role", Role.fromParam(role).getParam());
        return "users/login";
    }
}
