package com.rankedfinance.errors;

import java.util.List;

/**
 * A failure the API reports to the client: controllers and services throw this, and
 * ApiExceptionHandler turns it into a problem detail with the code's status. The detail is shown to
 * the user, so it must never hold secrets.
 */
public class ApiException extends RuntimeException {

    /** One invalid field, in the same shape bean validation produces in {@code errors[]}. */
    public record FieldError(String field, String message) {}

    private final ErrorCode code;
    private final List<FieldError> fieldErrors;

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
        this.fieldErrors = List.of();
    }

    private ApiException(ErrorCode code, String detail, List<FieldError> fieldErrors) {
        super(detail);
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    /** A validation_failed that names the field, for rules bean validation can't express. */
    public static ApiException invalidField(String field, String message) {
        return new ApiException(
                ErrorCode.VALIDATION_FAILED,
                "Some fields aren't valid.",
                List.of(new FieldError(field, message)));
    }

    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }

    public ErrorCode code() {
        return code;
    }
}
