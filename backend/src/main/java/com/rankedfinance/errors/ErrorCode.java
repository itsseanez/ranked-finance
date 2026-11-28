package com.rankedfinance.errors;

import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.HttpStatus;

/**
 * Every machine-readable {@code error} code the API sends (ADR 0014). Codes are only ever added,
 * never renamed or removed; a new one ships with its message in the frontend's lib/errors.ts.
 */
public enum ErrorCode {
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "unauthenticated"),
    SETUP_INCOMPLETE(HttpStatus.FORBIDDEN, "setup_incomplete"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden"),
    SETUP_CLOSED(HttpStatus.FORBIDDEN, "setup_closed"),
    SETUP_NOT_CONFIGURED(HttpStatus.FORBIDDEN, "setup_not_configured"),
    INVALID_SETUP_CODE(HttpStatus.UNAUTHORIZED, "invalid_setup_code"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "not_found"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "validation_failed"),
    CONFLICT(HttpStatus.CONFLICT, "conflict"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error"),
    // Shipped in 0.1.0, so kept (codes are never removed).
    LAST_PASSKEY(HttpStatus.CONFLICT, "last_passkey"),
    OWNER_MISMATCH(HttpStatus.CONFLICT, "owner_mismatch");

    private final HttpStatus status;
    private final String code;

    ErrorCode(HttpStatus status, String code) {
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    /** The code as it appears in JSON. */
    @JsonValue
    public String code() {
        return code;
    }

    /**
     * The code for a failure that only has a status (Boot's /error, a ResponseStatusException, a
     * Spring MVC exception).
     */
    static ErrorCode forStatus(int status) {
        return switch (status) {
            case 400 -> VALIDATION_FAILED;
            case 401 -> UNAUTHENTICATED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            default -> INTERNAL_ERROR;
        };
    }
}
