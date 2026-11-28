package com.rankedfinance.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rankedfinance.TestOwner;
import com.rankedfinance.TestcontainersConfiguration;
import com.rankedfinance.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, AccountRepository.class, UserRepository.class})
class AccountRepositoryTest {

    @Autowired AccountRepository accounts;
    @Autowired UserRepository users;
    @Autowired JdbcClient jdbc;

    UUID owner;

    @BeforeEach
    void createOwner() {
        owner = TestOwner.create(users);
    }

    private UUID cashAccount() {
        return accounts.insertManual(
                owner,
                new NewManualAccount(
                        "Checking",
                        "Credit Union",
                        AccountKind.CASH,
                        "USD",
                        null,
                        null,
                        UpdateReminder.MONTHLY));
    }

    private int snapshotRows(UUID accountId) {
        return jdbc.sql("select count(*) from balance_snapshots where account_id = :id")
                .param("id", accountId)
                .query(Integer.class)
                .single();
    }

    @Test
    void listShowsEachAccountsLatestBalance() {
        UUID id = cashAccount();
        accounts.upsertSnapshot(owner, id, LocalDate.of(2026, 11, 1), new BigDecimal("100.00"));
        accounts.upsertSnapshot(owner, id, LocalDate.of(2026, 11, 20), new BigDecimal("250.50"));

        Account account = accounts.findAll(owner).getFirst();

        assertThat(account.balance()).isEqualByComparingTo("250.50");
        assertThat(account.balanceAsOf()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(account.snapshotCount()).isEqualTo(2);
        assertThat(account.kind()).isEqualTo(AccountKind.CASH);
        assertThat(account.source()).isEqualTo(AccountSource.MANUAL);
        assertThat(accounts.findById(owner, id)).contains(account);
    }

    // Delete only needs to know 0, 1, or more, so the count stops at 2 instead of reading every
    // snapshot of a 10-year history on each list.
    @Test
    void snapshotCountIsCappedAtTwo() {
        UUID id = cashAccount();
        for (int day = 1; day <= 3; day++) {
            accounts.upsertSnapshot(owner, id, LocalDate.of(2026, 11, day), new BigDecimal("1.00"));
        }

        assertThat(accounts.findAll(owner).getFirst().snapshotCount()).isEqualTo(2);
    }

    @Test
    void sameDaySnapshotOverwrites() {
        UUID id = cashAccount();
        LocalDate day = LocalDate.of(2026, 11, 20);
        accounts.upsertSnapshot(owner, id, day, new BigDecimal("100.00"));
        accounts.upsertSnapshot(owner, id, day, new BigDecimal("300.00"));

        assertThat(snapshotRows(id)).isEqualTo(1);
        assertThat(accounts.findById(owner, id).orElseThrow().balance())
                .isEqualByComparingTo("300.00");
    }

    @Test
    void accountWithNoSnapshotsHasNoBalance() {
        UUID id = cashAccount();

        Account account = accounts.findById(owner, id).orElseThrow();

        assertThat(account.balance()).isNull();
        assertThat(account.balanceAsOf()).isNull();
        assertThat(account.snapshotCount()).isZero();
    }

    @Test
    void manualDebtWithoutAnAprIsRejected() {
        assertThatThrownBy(
                        () ->
                                accounts.insertManual(
                                        owner,
                                        new NewManualAccount(
                                                "Card",
                                                null,
                                                AccountKind.DEBT,
                                                "USD",
                                                null,
                                                null,
                                                UpdateReminder.MONTHLY)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAnAccountDeletesItsSnapshots() {
        UUID id = cashAccount();
        accounts.upsertSnapshot(owner, id, LocalDate.of(2026, 11, 1), new BigDecimal("100.00"));

        accounts.delete(owner, id);

        assertThat(accounts.findById(owner, id)).isEmpty();
        assertThat(snapshotRows(id)).isZero();
    }

    @Test
    void updateAndCloseChangeTheAccount() {
        UUID id = cashAccount();

        accounts.update(
                owner,
                id,
                "Savings",
                "Bank",
                new BigDecimal("4.250"),
                new BigDecimal("25.00"),
                UpdateReminder.NEVER,
                true);
        accounts.setClosedOn(owner, id, LocalDate.of(2026, 11, 28));

        Account account = accounts.findById(owner, id).orElseThrow();
        assertThat(account.name()).isEqualTo("Savings");
        assertThat(account.institutionName()).isEqualTo("Bank");
        assertThat(account.apr()).isEqualByComparingTo("4.25");
        assertThat(account.minimumPayment()).isEqualByComparingTo("25.00");
        assertThat(account.updateReminder()).isEqualTo(UpdateReminder.NEVER);
        assertThat(account.hidden()).isTrue();
        assertThat(account.closedOn()).isEqualTo(LocalDate.of(2026, 11, 28));

        accounts.setClosedOn(owner, id, null);
        assertThat(accounts.findById(owner, id).orElseThrow().closedOn()).isNull();
    }

    @Test
    void anotherUsersAccountIsInvisible() {
        UUID id = cashAccount();

        assertThat(accounts.findById(UUID.randomUUID(), id)).isEmpty();
        assertThat(accounts.findAll(UUID.randomUUID())).isEmpty();
    }
}
