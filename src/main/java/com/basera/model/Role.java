package com.basera.model;

/**
 * A user signs up as one of these (ROLES in the Node app's utils/constants.js).
 *
 * An enum is a type with a fixed set of values. A field of type Role can only ever hold HOST or CUSTOMER -
 * the compiler rejects anything else - so it does the job of Mongoose's `enum: Object.values(ROLES)`.
 *
 * The database stores the name (HOST). URLs and forms keep the Node app's lowercase strings
 * (/signup?role=host, <input name="role" value="host">), so fromParam() / getParam() translate between the two.
 */
public enum Role {
    HOST("Host"),         // the "admin" side: lists and manages places
    CUSTOMER("Customer"); // rents places

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** "host" -> HOST, anything else (including nothing) -> CUSTOMER. This is selectedRole() in controllers/users.js. */
    public static Role fromParam(String value) {
        return "host".equalsIgnoreCase(value) ? HOST : CUSTOMER;
    }

    /** HOST -> "host": the value used in URLs and form fields. */
    public String getParam() {
        return name().toLowerCase();
    }

    /** HOST -> "Host": the text shown on pages. */
    public String getLabel() {
        return label;
    }
}
