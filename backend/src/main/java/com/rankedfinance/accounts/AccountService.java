package com.rankedfinance.accounts;

import com.rankedfinance.errors.ApiException;
import com.rankedfinance.errors.ErrorCode;
import com.rankedfinance.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final AccountRepository accounts;
    private final UserRepository users;
    private final Clock clock;

    public AccountService(AccountRepository accounts, UserRepository users, Clock clock) {
        this.accounts = accounts;
        this.users = users;
        this.clock = clock;
    }

    /** Today in the user's time zone, never the server's. */
    private LocalDate today(UUID userId) {
        return LocalDate.now(clock.withZone(users.timeZone(userId)));
    }

    public List<AccountResponse> list(UUID userId) {
        LocalDate today = today(userId);
        // Open accounts first (false sorts before true), then institution and name.
        return accounts.findAll(userId).stream()
                .sorted(
                        Comparator.comparing((Account a) -> a.closedOn() != null)
                                .thenComparing(
                                        a -> a.institutionName() == null ? "" : a.institutionName(),
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(Account::name, String.CASE_INSENSITIVE_ORDER))
                .map(a -> toResponse(a, today))
                .toList();
    }

    public AccountHealthResponse health(UUID userId) {
        AccountHealth health = AccountHealth.summarize(accounts.findAll(userId), today(userId));
        return new AccountHealthResponse(health.upToDate(), health.needsAttention());
    }

    public List<AccountMatch> matches(UUID userId, CreateManualAccountRequest request) {
        Account candidate =
                new Account(
                        null,
                        AccountSource.MANUAL,
                        request.name(),
                        request.institutionName(),
                        null,
                        parseKind(request.kind()),
                        users.homeCurrency(userId),
                        null,
                        null,
                        UpdateReminder.MONTHLY,
                        false,
                        null,
                        null,
                        null,
                        0);
        return DuplicateAccountMatcher.matches(candidate, accounts.findAll(userId)).stream()
                .map(a -> new AccountMatch(a.id(), a.name()))
                .toList();
    }

    // @Transactional makes both writes commit together or not at all, like a DB transaction
    // wrapped around this method: an exception rolls everything back.
    @Transactional
    public AccountResponse createManual(UUID userId, CreateManualAccountRequest request) {
        AccountKind kind = parseKind(request.kind());
        BigDecimal balance = parseMoney("balance", request.balance());
        if (kind != AccountKind.CASH && balance.signum() < 0) {
            throw ApiException.invalidField("balance", "Only cash can be negative.");
        }
        BigDecimal apr = parseApr(request.apr());
        if (kind == AccountKind.DEBT && apr == null) {
            throw ApiException.invalidField("apr", "A debt needs an interest rate.");
        }
        BigDecimal minimumPayment = parseOptionalMoney("minimumPayment", request.minimumPayment());
        UpdateReminder reminder =
                request.updateReminder() == null
                        ? UpdateReminder.MONTHLY
                        : parseReminder(request.updateReminder());
        UUID id =
                accounts.insertManual(
                        userId,
                        new NewManualAccount(
                                request.name().strip(),
                                blankToNull(request.institutionName()),
                                kind,
                                users.homeCurrency(userId),
                                apr,
                                minimumPayment,
                                reminder));
        accounts.upsertSnapshot(userId, id, today(userId), balance);
        return respond(userId, id);
    }

    @Transactional
    public AccountResponse setBalance(UUID userId, UUID id, BalanceRequest request) {
        Account account = find(userId, id);
        requireOpenManual(account);
        BigDecimal balance = parseMoney("balance", request.balance());
        if (account.kind() != AccountKind.CASH && balance.signum() < 0) {
            throw ApiException.invalidField("balance", "Only cash can be negative.");
        }
        accounts.upsertSnapshot(userId, id, today(userId), balance);
        return respond(userId, id);
    }

    @Transactional
    public AccountResponse update(UUID userId, UUID id, UpdateAccountRequest request) {
        Account account = find(userId, id);
        if (account.closedOn() != null) {
            throw new ApiException(ErrorCode.CONFLICT, "Closed accounts can't be changed.");
        }
        String name = account.name();
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw ApiException.invalidField("name", "Name can't be blank.");
            }
            name = request.name().strip();
        }
        BigDecimal apr = request.apr() == null ? account.apr() : parseApr(request.apr());
        if (account.kind() == AccountKind.DEBT
                && account.source() == AccountSource.MANUAL
                && apr == null) {
            throw ApiException.invalidField("apr", "A debt needs an interest rate.");
        }
        accounts.update(
                userId,
                id,
                name,
                request.institutionName() == null
                        ? account.institutionName()
                        : blankToNull(request.institutionName()),
                apr,
                request.minimumPayment() == null
                        ? account.minimumPayment()
                        : parseOptionalMoney("minimumPayment", request.minimumPayment()),
                request.updateReminder() == null
                        ? account.updateReminder()
                        : parseReminder(request.updateReminder()),
                request.hidden() == null ? account.hidden() : request.hidden());
        return respond(userId, id);
    }

    @Transactional
    public AccountResponse close(UUID userId, UUID id, LocalDate closedOn) {
        find(userId, id);
        if (closedOn.isAfter(today(userId))) {
            throw ApiException.invalidField("closedOn", "The close date can't be in the future.");
        }
        accounts.setClosedOn(userId, id, closedOn);
        return respond(userId, id);
    }

    @Transactional
    public AccountResponse reopen(UUID userId, UUID id) {
        find(userId, id);
        accounts.setClosedOn(userId, id, null);
        return respond(userId, id);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Account account = find(userId, id);
        if (!canDelete(account)) {
            throw new ApiException(
                    ErrorCode.CONFLICT, "An account with balance history can't be deleted.");
        }
        accounts.delete(userId, id);
    }

    private Account find(UUID userId, UUID id) {
        return accounts.findById(userId, id)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Account not found."));
    }

    private AccountResponse respond(UUID userId, UUID id) {
        return toResponse(find(userId, id), today(userId));
    }

    private static void requireOpenManual(Account account) {
        if (account.closedOn() != null) {
            throw new ApiException(ErrorCode.CONFLICT, "Closed accounts can't be changed.");
        }
        if (account.source() != AccountSource.MANUAL) {
            throw new ApiException(
                    ErrorCode.CONFLICT, "Only manual accounts have balances you can set.");
        }
    }

    private static boolean canDelete(Account account) {
        return account.snapshotCount() <= 1;
    }

    private static AccountResponse toResponse(Account a, LocalDate today) {
        return new AccountResponse(
                a.id(),
                lower(a.source()),
                a.name(),
                a.institutionName(),
                lower(a.kind()),
                a.currency(),
                money(a.balance()),
                a.balanceAsOf(),
                a.apr() == null ? null : a.apr().toPlainString(),
                money(a.minimumPayment()),
                lower(a.updateReminder()),
                a.hidden(),
                a.closedOn(),
                UpdateReminders.needsUpdate(a, today),
                canDelete(a));
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static String money(BigDecimal value) {
        return value == null ? null : value.setScale(2).toPlainString();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static AccountKind parseKind(String value) {
        try {
            return AccountKind.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidField("kind", "Unknown account kind.");
        }
    }

    private static UpdateReminder parseReminder(String value) {
        try {
            return UpdateReminder.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.invalidField("updateReminder", "Unknown reminder.");
        }
    }

    // Checked before new BigDecimal(...): "1e99999999" would otherwise make setScale build a
    // huge number and exhaust memory. At most 12 integer digits fits numeric(14,2).
    private static final Pattern MONEY = Pattern.compile("[+-]?\\d{1,12}(\\.\\d{1,2})?");
    private static final Pattern APR = Pattern.compile("\\d{1,3}(\\.\\d{1,3})?");

    /** Parses a plain decimal string: no exponent, at most 2 decimals, fits numeric(14,2). */
    private static BigDecimal parseMoney(String field, String text) {
        String stripped = text.strip();
        if (!MONEY.matcher(stripped).matches()) {
            throw ApiException.invalidField(
                    field, "Enter an amount like 1234.56 (up to 12 digits, 2 decimals).");
        }
        return new BigDecimal(stripped).setScale(2);
    }

    private static BigDecimal parseOptionalMoney(String field, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        BigDecimal value = parseMoney(field, text);
        if (value.signum() < 0) {
            throw ApiException.invalidField(field, "Can't be negative.");
        }
        return value;
    }

    /** APR is a percentage, 0 to 100, stored with up to 3 decimals (numeric(6,3)). */
    private static BigDecimal parseApr(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String stripped = text.strip();
        if (!APR.matcher(stripped).matches()) {
            throw ApiException.invalidField("apr", "Enter a rate like 19.99 (up to 3 decimals).");
        }
        BigDecimal value = new BigDecimal(stripped);
        if (value.compareTo(HUNDRED) > 0) {
            throw ApiException.invalidField("apr", "The rate must be between 0 and 100.");
        }
        return value;
    }
}
