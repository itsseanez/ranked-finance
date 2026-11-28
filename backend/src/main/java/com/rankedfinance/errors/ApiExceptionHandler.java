package com.rankedfinance.errors;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every exception a controller throws into a problem detail with an {@code error} code. A
 * {@code @RestControllerAdvice} applies its {@code @ExceptionHandler} methods to all controllers,
 * like an Express error middleware. ResponseEntityExceptionHandler already maps Spring MVC's own
 * exceptions (bad JSON, wrong method, ResponseStatusException) to problem details; this adds the
 * code. Replaces the handler Boot registers for spring.mvc.problemdetails.enabled.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException ex) {
        ProblemDetail problem = ApiProblems.of(ex.code(), ex.getMessage());
        if (!ex.fieldErrors().isEmpty()) {
            problem.setProperty("errors", ex.fieldErrors());
        }
        return ResponseEntity.status(ex.code().status()).body(problem);
    }

    /**
     * Spring Security's exceptions go back to its ExceptionTranslationFilter, which owns the 401
     * and 403 answers (ProblemSecurityHandlers). Rethrowing the same exception tells Spring MVC
     * this handler didn't resolve it, so it propagates out of the servlet.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void handleSecurityException(RuntimeException ex) {
        throw ex;
    }

    /** Anything else is a bug: log it here, and never send its message to the client. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.status())
                .body(ApiProblems.of(code, "Something went wrong."));
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(
                                error ->
                                        Map.of(
                                                "field",
                                                error.getField(),
                                                "message",
                                                String.valueOf(error.getDefaultMessage())))
                        .toList();
        ProblemDetail problem = ex.getBody();
        problem.setDetail("Some fields aren't valid.");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Every Spring MVC exception passes through here; give its problem a code by status. */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {
        ProblemDetail problem =
                body instanceof ProblemDetail detail
                        ? detail
                        : ApiProblems.forStatus(statusCode.value(), null);
        problem.setProperty("error", ErrorCode.forStatus(statusCode.value()));
        return super.handleExceptionInternal(ex, problem, headers, statusCode, request);
    }
}
