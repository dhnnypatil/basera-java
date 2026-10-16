package com.basera.api;

import java.time.temporal.Temporal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.basera.exception.AppException;
import com.basera.web.FormErrors;

import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Turns every error from an /api controller into JSON: { "error": "message" } with the right status code
 * (the isApiRequest branch of the error handler in app.js).
 *
 * basePackages = "com.basera.api" limits it to the API controllers; the website has web/WebExceptionHandler.
 * @Order(HIGHEST_PRECEDENCE) makes Spring ask this class first.
 */
@RestControllerAdvice(basePackages = "com.basera.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, String>> appException(AppException e) {
        return error(e.getStatus(), e.getMessage());
    }

    /** Validation failed (@Valid). Same joined message as the website: "price is required, title is ...". */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> invalid(BindException e) {
        return error(HttpStatus.BAD_REQUEST, FormErrors.message(e.getBindingResult()));
    }

    /** The body isn't valid JSON, or a value has the wrong type ("price": "abc"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().getLast().getPropertyName();
            Class<?> type = mismatch.getTargetType();
            String problem = type != null && Number.class.isAssignableFrom(type) || type == int.class ? "must be a number"
                    : type != null && Temporal.class.isAssignableFrom(type) ? "must be in ISO 8601 date format"
                    : "has the wrong type";
            return error(HttpStatus.BAD_REQUEST, field + " " + problem);
        }
        return error(HttpStatus.BAD_REQUEST, "The request body must be valid JSON.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> wrongContentType(HttpMediaTypeNotSupportedException e) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Send the body as JSON (Content-Type: application/json).");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> wrongMethod(HttpRequestMethodNotSupportedException e) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "This URL doesn't support " + e.getMethod() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> badUrlValue(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.NOT_FOUND, "Not found.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> uploadTooLarge(MaxUploadSizeExceededException e) {
        return error(HttpStatus.BAD_REQUEST, "Upload error: the photo must be 5 MB or smaller.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> unexpected(Exception e) {
        log.error("Unexpected API error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
