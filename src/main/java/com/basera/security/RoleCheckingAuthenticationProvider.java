package com.basera.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Checks a username + password login (passport-local's LocalStrategy), plus the role tab.
 *
 * DaoAuthenticationProvider is Spring's standard "look the user up, compare the password hash" logic.
 * We extend it and add one rule after the password check, in the same order as controllers/users.js:
 * a wrong password always says "Invalid username or password", and only then is the tab checked.
 */
public class RoleCheckingAuthenticationProvider extends DaoAuthenticationProvider {

    public RoleCheckingAuthenticationProvider(BaseraUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        super(userDetailsService);
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) {
        super.additionalAuthenticationChecks(userDetails, authentication); // the password check

        // `instanceof LoginDetails details` checks the type and names the variable in one step.
        // Logins that don't send a tab (like the JSON API in phase 6) skip the role check, as in the Node app.
        CurrentUser user = (CurrentUser) userDetails;
        if (authentication.getDetails() instanceof LoginDetails details && details.requestedRole() != user.getRole()) {
            throw new WrongRoleException(user.getRole());
        }
    }
}
