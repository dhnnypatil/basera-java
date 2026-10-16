package com.basera.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.basera.repository.UserRepository;

/**
 * Tells Spring Security how to find a user by username. It's the lookup half of passport-local-mongoose's
 * User.authenticate(): Spring then compares the typed password with the stored BCrypt hash itself.
 *
 * Used twice: by the login form (RoleCheckingAuthenticationProvider) and by the remember-me cookie, which
 * reloads the user after a restart. Because this bean exists, Spring Boot no longer creates its built-in
 * test user - that's what removes the "Using generated security password" warning at startup.
 */
@Service
public class BaseraUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public BaseraUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public CurrentUser loadUserByUsername(String username) {
        return users.findByUsername(username)
                .map(CurrentUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user called " + username));
    }
}
