package com.rankedfinance.user;

import com.rankedfinance.errors.ApiException;
import com.rankedfinance.errors.ErrorCode;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class MeController {

    private final UserRepository users;

    MeController(UserRepository users) {
        this.users = users;
    }

    /** The signed-in user. */
    @GetMapping("/me")
    MeResponse me(CurrentUser current) {
        return users.findById(current.id())
                .map(
                        user ->
                                new MeResponse(
                                        user.id(),
                                        user.displayName(),
                                        user.email(),
                                        user.timeZone()))
                .orElseThrow(
                        () -> new ApiException(ErrorCode.NOT_FOUND, "Your account wasn't found."));
    }

    /**
     * Sets the user's time zone. The frontend sends the browser's zone after sign-in when it
     * differs, so "today" (and the date on every balance) matches where the user is.
     */
    @PutMapping("/me/time-zone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setTimeZone(CurrentUser current, @RequestBody TimeZoneRequest request) {
        users.updateTimeZone(current.id(), parseZone(request.timeZone()).getId());
    }

    private static ZoneId parseZone(String value) {
        if (value == null || value.isBlank()) {
            throw ApiException.invalidField("timeZone", "Time zone is required.");
        }
        try {
            return ZoneId.of(value);
        } catch (DateTimeException e) {
            throw ApiException.invalidField("timeZone", "Unknown time zone.");
        }
    }
}
