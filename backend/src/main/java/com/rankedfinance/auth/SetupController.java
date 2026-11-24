package com.rankedfinance.auth;

import com.rankedfinance.common.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * First-run setup (ADR 0010, 0013). Until a passkey exists, the right setup code creates the owner
 * and opens a setup-only session, so they can register their first passkey through Spring
 * Security's endpoints and then sign in with it.
 */
@RestController
@RequestMapping("/api/v1/setup")
class SetupController {

    record SetupRequest(String code) {
        // Spring MVC logs parsed request bodies at DEBUG; keep the code out of the logs.
        @Override
        public String toString() {
            return "SetupRequest[code=<redacted>]";
        }
    }

    private final FirstRunSetup firstRunSetup;
    private final SetupSession setupSession;

    SetupController(FirstRunSetup firstRunSetup, SetupSession setupSession) {
        this.firstRunSetup = firstRunSetup;
        this.setupSession = setupSession;
    }

    @PostMapping
    ResponseEntity<ProblemDetail> setup(
            @RequestBody SetupRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        return switch (firstRunSetup.start(body.code())) {
            case FirstRunSetup.Closed closed ->
                    ApiProblems.response(
                            HttpStatus.FORBIDDEN, "setup_closed", "This app is already set up.");
            case FirstRunSetup.WrongCode wrongCode ->
                    ApiProblems.response(
                            HttpStatus.UNAUTHORIZED,
                            "invalid_setup_code",
                            "That setup code isn't right.");
            case FirstRunSetup.OwnerMismatch mismatch ->
                    ApiProblems.response(
                            HttpStatus.CONFLICT,
                            "owner_mismatch",
                            "OWNER_EMAIL doesn't match the owner already on this server.");
            case FirstRunSetup.Opened opened -> {
                setupSession.start(opened.email(), request, response);
                yield ResponseEntity.noContent().build();
            }
        };
    }
}
