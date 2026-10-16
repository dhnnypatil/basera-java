package com.basera.exception;

import org.springframework.http.HttpStatus;

/**
 * An error with an HTTP status and a message that is safe to show to the user (utils/ExpressError.js).
 *
 *   Node:  throw new ExpressError(409, "That username is already taken.");
 *   Java:  throw new AppException(HttpStatus.CONFLICT, "That username is already taken.");
 *
 * It extends RuntimeException, an "unchecked" exception: methods can throw it without declaring
 * `throws AppException`, which keeps service code as short as the Node version.
 *
 * What happens next is decided in one place (like the error handler at the bottom of app.js):
 *   - JSON API          -> { "error": message } with the status (api/ApiExceptionHandler)
 *   - website           -> flash the message and redirect, or show the error page (web/WebExceptionHandler)
 * `redirectTo` is for the permission checks from middleware/auth.js, which sent the website to a specific
 * page, e.g. deny(..., 403, "You are not the owner of this listing.", `/listings/${id}`).
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String redirectTo;

    public AppException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public AppException(HttpStatus status, String message, String redirectTo) {
        super(message);
        this.status = status;
        this.redirectTo = redirectTo;
    }

    public static AppException notFound(String message, String redirectTo) {
        return new AppException(HttpStatus.NOT_FOUND, message, redirectTo);
    }

    public static AppException forbidden(String message, String redirectTo) {
        return new AppException(HttpStatus.FORBIDDEN, message, redirectTo);
    }

    public static AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Where the website should send the user (with the message flashed), or null to use the default. */
    public String getRedirectTo() {
        return redirectTo;
    }
}
