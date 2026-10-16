package com.basera.security;

import org.springframework.security.core.AuthenticationException;

import com.basera.model.Role;

/**
 * Login failed because the password was right but the wrong tab was picked
 * (the `user.role !== role` branch of login() in controllers/users.js).
 * AuthRedirects.onAuthenticationFailure turns it into the "This is a Host account ..." message.
 */
public class WrongRoleException extends AuthenticationException {

    private final Role actualRole;

    public WrongRoleException(Role actualRole) {
        super("This account's role is " + actualRole);
        this.actualRole = actualRole;
    }

    public Role getActualRole() {
        return actualRole;
    }
}
