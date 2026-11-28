package com.rankedfinance.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
