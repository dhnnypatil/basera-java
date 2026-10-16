package com.basera.security;

import java.io.Serializable;

import com.basera.model.Role;

/**
 * Extra information sent with the login form: which tab (Customer / Host) the user picked.
 *
 * Spring Security's login filter only reads `username` and `password`. SecurityConfig tells it to also build
 * one of these from the request (authenticationDetailsSource), and RoleCheckingAuthenticationProvider
 * compares it with the account's real role.
 *
 * A record is a short way to write a class that only holds data: Java generates the constructor,
 * the requestedRole() getter, equals() and toString(). Serializable because it ends up in the session.
 */
public record LoginDetails(Role requestedRole) implements Serializable {
}
