package com.basera.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.basera.model.Role;
import com.basera.model.User;

/**
 * The logged-in user, as Spring Security keeps it in the session. This is the Node app's `req.user`.
 *
 * Passport stored only the user id in the session and loaded the full user from MongoDB on every request
 * (deserializeUser). Spring Security stores this small object in the session instead, so ordinary requests
 * don't hit the database. It holds only what pages and permission checks need, never the whole User.
 *
 * UserDetails is the interface Spring Security uses for "a user who can log in".
 * In templates it's `currUser` (see web/GlobalModelAttributes), e.g. ${currUser.username}, ${currUser.host}.
 */
public class CurrentUser implements UserDetails, CredentialsContainer {

    private final String id;
    private final String username;
    private final String email;
    private final Role role;
    private String password; // the BCrypt hash; erased once login has finished (see eraseCredentials)

    public CurrentUser(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.password = user.getPassword();
    }

    /**
     * Roles become "authorities": HOST -> ROLE_HOST. SecurityConfig's hasRole("HOST") checks for exactly this
     * name - the ROLE_ prefix is Spring Security's convention for roles.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Spring Security calls this after a successful login, so the password hash doesn't sit in the session.
     * (Spring's own User class does the same.)
     */
    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public boolean isHost() {
        return role == Role.HOST;
    }

    public boolean isCustomer() {
        return role == Role.CUSTOMER;
    }
}
