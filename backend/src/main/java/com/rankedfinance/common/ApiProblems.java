package com.rankedfinance.common;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

/**
 * API errors as RFC 9457 problem details, with a machine-readable {@code error} code the frontend
 * switches on (CLAUDE.md: errors use Problem Details). Every error goes through here so they all
 * have the same shape.
 */
public final class ApiProblems {

    private ApiProblems() {}

    public static ProblemDetail of(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("error", code);
        return problem;
    }

    /** For controllers. */
    public static ResponseEntity<ProblemDetail> response(
            HttpStatus status, String code, String detail) {
        return ResponseEntity.status(status).body(of(status, code, detail));
    }

    /**
     * For servlet filters, which run before Spring MVC and so can't return a ProblemDetail. Writes
     * the same JSON a controller's response would produce.
     */
    public static void write(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String detail,
            JsonMapper json)
            throws IOException {
        ProblemDetail problem = of(status, code, detail);
        // Optional members are left out when unset, as in a controller's response. (Spring
        // Framework 7 leaves type null unless set; RFC 9457 reads a missing type as about:blank.)
        Map<String, Object> body = new LinkedHashMap<>();
        if (problem.getType() != null) {
            body.put("type", problem.getType().toString());
        }
        if (problem.getTitle() != null) {
            body.put("title", problem.getTitle());
        }
        body.put("status", problem.getStatus());
        if (problem.getDetail() != null) {
            body.put("detail", problem.getDetail());
        }
        body.putAll(problem.getProperties());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getWriter(), body);
    }
}
