package com.rankedfinance.errors;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.json.JsonMapper;

/**
 * API errors as RFC 9457 problem details, with a machine-readable {@code error} code the frontend
 * switches on (ADR 0014). Every error goes through here so they all have the same shape.
 */
public final class ApiProblems {

    private ApiProblems() {}

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setProperty("error", code);
        return problem;
    }

    /**
     * A problem for a failure that only has a status, coded by {@link ErrorCode#forStatus}. It
     * keeps the real status (a 415 stays a 415) even when the code falls back to internal_error.
     */
    static ProblemDetail forStatus(int status, String detail) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setProperty("error", ErrorCode.forStatus(status));
        return problem;
    }

    /**
     * For servlet filters, which run before Spring MVC and so can't return a ProblemDetail. Writes
     * the same JSON a controller's response would produce.
     */
    public static void write(
            HttpServletResponse response, ErrorCode code, String detail, JsonMapper json)
            throws IOException {
        ProblemDetail problem = of(code, detail);
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
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(response.getWriter(), body);
    }
}
