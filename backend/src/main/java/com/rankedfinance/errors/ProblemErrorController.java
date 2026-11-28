package com.rankedfinance.errors;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The servlet container forwards any error nothing else answered (a filter's sendError, an
 * exception outside Spring MVC) to /error. Implementing Boot's ErrorController replaces Boot's
 * BasicErrorController, so the answer is a problem detail with a code instead of Boot's default
 * body, and never a stack trace.
 */
@Hidden // Not part of the API contract.
@RestController
class ProblemErrorController implements ErrorController {

    @RequestMapping("/error")
    ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        int status =
                request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) instanceof Integer code
                        ? code
                        : 500;
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ApiProblems.forStatus(status, null));
    }
}
