package com.basera.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import com.basera.TestData;
import com.basera.TestMongo;
import com.basera.model.Role;
import com.basera.model.User;
import com.basera.security.CurrentUser;

/**
 * Who may open which URL (config/SecurityConfig + middleware/auth.js), for the website and the JSON API.
 *
 * MockMvc sends fake HTTP requests straight into the app - no browser, no real network - and checks the
 * responses. It's like supertest for Express.
 */
@SpringBootTest(properties = { "basera.seed=never", "basera.geocoding.enabled=false" })
@AutoConfigureMockMvc
@Import(TestMongo.class)
class SecurityRulesTest {

    @Autowired MockMvc mvc;
    @Autowired TestData data;

    User host;
    User customer;

    @BeforeEach
    void setUp() {
        data.clear();
        host = data.user("host1", Role.HOST);
        customer = data.user("cust1", Role.CUSTOMER);
    }

    // ----- Website -----

    @Test
    void publicPagesAreOpenToEveryone() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/listings")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/signup")).andExpect(status().isOk());
    }

    @Test
    void visitorsAreSentToLogInFirst() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flashMessage("error", "Please log in first."));
        mvc.perform(get("/bookings")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/listings/new")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void customersCantOpenHostPagesAndHostsCantOpenCustomerPages() throws Exception {
        mvc.perform(get("/dashboard").with(as(customer)))
                .andExpect(redirectedUrl("/listings"))
                .andExpect(flashMessage("error", "Only hosts can do that. Log in with a Host account."));
        mvc.perform(get("/bookings").with(as(host)))
                .andExpect(redirectedUrl("/listings"))
                .andExpect(flashMessage("error", "Only customers can do that. Log in with a Customer account."));
        mvc.perform(get("/dashboard").with(as(host))).andExpect(status().isOk());
        mvc.perform(get("/bookings").with(as(customer))).andExpect(status().isOk());
    }

    @Test
    void formsWithoutACsrfTokenAreRejected() throws Exception {
        mvc.perform(post("/logout").with(as(host)))
                .andExpect(redirectedUrl("/"))
                .andExpect(flashMessage("error", "Your session expired - please try again."));
    }

    @Test
    void loginChecksThePasswordAndTheRoleTab() throws Exception {
        mvc.perform(post("/login").with(csrf()).param("username", "host1").param("password", TestData.PASSWORD).param("role", "host"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flashMessage("success", "Welcome back, host1!"));
        mvc.perform(post("/login").with(csrf()).param("username", "host1").param("password", "wrong").param("role", "host"))
                .andExpect(redirectedUrl("/login?role=host"))
                .andExpect(flashMessage("error", "Invalid username or password."));
        mvc.perform(post("/login").with(csrf()).param("username", "host1").param("password", TestData.PASSWORD).param("role", "customer"))
                .andExpect(redirectedUrl("/login?role=host"))
                .andExpect(flashMessage("error", "This is a Host account - please use the Host tab to log in."));
    }

    @Test
    void signupValidatesAndLogsTheNewUserIn() throws Exception {
        mvc.perform(post("/signup").with(csrf()).param("username", "").param("email", "bad").param("password", "1").param("role", "host"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "email must be a valid email, password length must be at least 6 characters long, username is not allowed to be empty")));
        mvc.perform(post("/signup").with(csrf()).param("username", "newhost").param("email", "New@Example.com").param("password", "secret1").param("role", "host"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flashMessage("success", "Welcome to Basera, newhost!"));
        mvc.perform(post("/signup").with(csrf()).param("username", "other").param("email", "new@example.com").param("password", "secret1").param("role", "host"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("An account with that email already exists.")));
    }

    // ----- JSON API -----

    @Test
    void apiAnswersWithJsonErrors() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please log in first."));
        mvc.perform(post("/api/listings").with(as(customer)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only hosts can do that. Log in with a Host account."));
        mvc.perform(post("/api/bookings").with(as(host)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/listings/000000000000000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Listing not found."));
    }

    @Test
    void apiLoginNeedsNoCsrfTokenAndNeverReturnsThePasswordHash() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cust1\",\"password\":\"" + TestData.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("cust1"))
                .andExpect(jsonPath("$.user.role").value("customer"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"cust1\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password."));
    }

    /** Sends the request as this user, as if they had logged in. */
    private static RequestPostProcessor as(User user) {
        CurrentUser principal = TestData.login(user);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    /**
     * Checks the one-time message saved for the next page. Messages from controllers and from the security
     * handlers (web/Flash) both end up in the session, so that's where we look.
     */
    @SuppressWarnings("unchecked")
    private static ResultMatcher flashMessage(String type, String message) {
        return result -> {
            var session = result.getRequest().getSession(false);
            assertThat(session).as("a session holding the flash message").isNotNull();
            var maps = (List<FlashMap>) session.getAttribute(SessionFlashMapManager.class.getName() + ".FLASH_MAPS");
            assertThat(maps).as("flash messages").isNotNull();
            assertThat(maps).anySatisfy(map -> assertThat(map.get(type)).isEqualTo(message));
        };
    }
}
