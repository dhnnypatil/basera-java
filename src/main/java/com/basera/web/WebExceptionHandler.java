package com.basera.web;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.basera.exception.AppException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The website's error handler - the errorHandler function at the bottom of app.js.
 *
 * @ControllerAdvice + @ExceptionHandler(X.class): whenever a controller (or Spring itself) throws an X,
 * Spring calls that method instead of crashing, like Express's `app.use((err, req, res, next) => ...)`.
 *
 * Same rules as the Node app:
 *   - a known 4xx error from a form (POST/PUT/DELETE)  -> flash the message and go back to the page
 *   - a permission check with a target page            -> flash the message and go to that page
 *   - anything else                                    -> render pages/error.html with the status
 * Errors from /api controllers are turned into JSON by api/ApiExceptionHandler, which runs first. The
 * isApi() checks here are for the two errors that happen before any controller is chosen (unknown URL,
 * upload too large).
 */
@ControllerAdvice
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ModelAndView appException(AppException e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (e.getRedirectTo() != null) {
            Flash.redirect(request, response, e.getRedirectTo(), "error", e.getMessage());
            return null;
        }
        return respond(e.getStatus(), e.getMessage(), request, response);
    }

    /** No controller and no static file for this URL. */
    @ExceptionHandler({ NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class })
    public ModelAndView notFound(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return respond(HttpStatus.NOT_FOUND, "Page not found.", request, response);
    }

    /** e.g. /listings/abc/edit?x when an id or number in the URL has the wrong format (Mongoose's CastError). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView badUrlValue(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return respond(HttpStatus.NOT_FOUND, "Not found.", request, response);
    }

    /** The photo is over spring.servlet.multipart.max-file-size (multer's MulterError). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView uploadTooLarge(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return respond(HttpStatus.BAD_REQUEST, "Upload error: the photo must be 5 MB or smaller.", request, response);
    }

    /** Anything we didn't expect: log the details for us, show a generic message to the user. */
    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.", request, response);
    }

    private ModelAndView respond(HttpStatus status, String message, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (isApi(request)) {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"" + message.replace("\"", "\\\"") + "\"}");
            return null;
        }
        if (status.is4xxClientError() && !"GET".equals(request.getMethod())) {
            Flash.redirect(request, response, Flash.backUrl(request), "error", message);
            return null;
        }
        // The page's model uses "errorStatus"/"errorMessage" (not "error"), because includes/flash.html
        // already shows an attribute called "error" as a red flash message.
        ModelAndView page = new ModelAndView("pages/error");
        page.setStatus(status);
        page.addObject("errorStatus", status.value());
        page.addObject("errorMessage", message);
        return page;
    }

    /** Is this request for the JSON API (utils/helpers.js isApiRequest)? */
    static boolean isApi(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/") || request.getRequestURI().equals("/api");
    }
}
