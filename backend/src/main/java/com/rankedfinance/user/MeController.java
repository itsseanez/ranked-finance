package com.rankedfinance.user;

import java.security.Principal;
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

    /** The signed-in user. The principal's name is their email (see OwnerUserDetailsService). */
    @GetMapping("/me")
    MeResponse me(Principal principal) {
        return users.findByEmail(principal.getName())
                .map(user -> new MeResponse(user.id(), user.displayName(), user.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
