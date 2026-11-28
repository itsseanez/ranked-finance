package com.rankedfinance.accounts;

import com.rankedfinance.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController {

    private final AccountService service;

    AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    List<AccountResponse> list(CurrentUser user) {
        return service.list(user.id());
    }

    @GetMapping("/health")
    AccountHealthResponse health(CurrentUser user) {
        return service.health(user.id());
    }

    @PostMapping("/matches")
    List<AccountMatch> matches(CurrentUser user, @Valid @RequestBody CreateManualAccountRequest r) {
        return service.matches(user.id(), r);
    }

    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    AccountResponse createManual(
            CurrentUser user, @Valid @RequestBody CreateManualAccountRequest r) {
        return service.createManual(user.id(), r);
    }

    @PutMapping("/{id}/balance")
    AccountResponse setBalance(
            CurrentUser user, @PathVariable UUID id, @Valid @RequestBody BalanceRequest r) {
        return service.setBalance(user.id(), id, r);
    }

    @PatchMapping("/{id}")
    AccountResponse update(
            CurrentUser user, @PathVariable UUID id, @RequestBody UpdateAccountRequest r) {
        return service.update(user.id(), id, r);
    }

    @PostMapping("/{id}/close")
    AccountResponse close(
            CurrentUser user, @PathVariable UUID id, @Valid @RequestBody CloseAccountRequest r) {
        return service.close(user.id(), id, r.closedOn());
    }

    @PostMapping("/{id}/reopen")
    AccountResponse reopen(CurrentUser user, @PathVariable UUID id) {
        return service.reopen(user.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(CurrentUser user, @PathVariable UUID id) {
        service.delete(user.id(), id);
    }
}
