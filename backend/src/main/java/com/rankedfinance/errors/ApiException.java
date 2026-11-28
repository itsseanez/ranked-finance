package com.rankedfinance.errors;

/**
 * A failure the API reports to the client: controllers and services throw this, and
 * ApiExceptionHandler turns it into a problem detail with the code's status. The detail is shown to
 * the user, so it must never hold secrets.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code, String detail) {
        super(detail);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
