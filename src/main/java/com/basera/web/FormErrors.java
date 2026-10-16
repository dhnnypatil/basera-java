package com.basera.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

/**
 * Turns validation errors into one message, the way middleware/validate.js joined Joi's messages:
 *   "username is not allowed to be empty, password length must be at least 6 characters long"
 */
public final class FormErrors {

    /** The annotations that mean "this field is empty" (an error's code is its annotation's name). */
    private static final Set<String> EMPTY_CODES = Set.of("NotBlank", "NotEmpty", "NotNull");

    private FormErrors() {
    }

    public static String message(BindingResult result) {
        // Group the errors by field name; TreeMap keeps the fields in alphabetical order.
        Map<String, List<FieldError>> byField = result.getFieldErrors().stream()
                .collect(Collectors.groupingBy(FieldError::getField, TreeMap::new, Collectors.toList()));

        List<String> messages = new ArrayList<>();
        byField.forEach((field, errors) -> {
            // An empty field also breaks the length rule. Joi only reported "is not allowed to be empty",
            // so when that error is there, it's the only one we keep for the field.
            errors.stream()
                    .filter(error -> EMPTY_CODES.contains(error.getCode()))
                    .findFirst()
                    .ifPresentOrElse(
                            error -> messages.add(text(error, result)),
                            () -> errors.stream().map(error -> text(error, result)).sorted().forEach(messages::add));
        });
        return String.join(", ", messages);
    }

    /**
     * The message for one error. "typeMismatch" means Spring couldn't convert the text at all (price=abc,
     * checkIn=tomorrow); its built-in message is a long technical sentence, so we use Joi's wording instead.
     */
    private static String text(FieldError error, BindingResult result) {
        if (!"typeMismatch".equals(error.getCode())) {
            return error.getDefaultMessage();
        }
        String field = error.getField().substring(error.getField().lastIndexOf('.') + 1);
        Class<?> type = result.getFieldType(error.getField());
        if (type != null && Number.class.isAssignableFrom(type)) {
            return field + " must be a number";
        }
        if (type != null && java.time.temporal.Temporal.class.isAssignableFrom(type)) {
            return field + " must be in ISO 8601 date format";
        }
        return field + " is not valid";
    }
}
