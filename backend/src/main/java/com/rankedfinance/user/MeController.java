package com.rankedfinance.user;

import com.rankedfinance.errors.ApiException;
import com.rankedfinance.errors.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
                .map(user -> new MeResponse(user.id(), user.displayName(), user.email()))
                .orElseThrow(
                        () -> new ApiException(ErrorCode.NOT_FOUND, "Your account wasn't found."));
    }
}
