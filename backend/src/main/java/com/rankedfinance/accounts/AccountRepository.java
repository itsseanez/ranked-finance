package com.rankedfinance.accounts;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Every method takes the user id first and scopes its SQL to it. */
@Repository
public class AccountRepository {

    // "left join lateral" runs the subquery once per account row, so it can see a.id: here it
    // picks each account's newest snapshot (null columns when there is none). Like a
    // per-item .find() in JS, but done in SQL. The count is a second scalar subquery.
    private static final String SELECT =
            """
            select a.id, a.source, a.name, a.institution_name, a.last4, a.kind, a.currency, a.apr,
                   a.minimum_payment, a.update_reminder, a.hidden, a.closed_on,
                   latest.balance, latest.date as balance_as_of,
                   (select count(*) from balance_snapshots s where s.account_id = a.id)
                       as snapshot_count
            from accounts a
            left join lateral (
                select s.balance, s.date from balance_snapshots s
                where s.account_id = a.id
                order by s.date desc
                limit 1
            ) latest on true
            where a.user_id = :userId
            """;

    // JdbcClient hands each result row to a RowMapper, like a .map(row => ...) callback in JS.
    private static final RowMapper<Account> ROW_MAPPER =
            (rs, rowNum) -> {
                Date balanceAsOf = rs.getDate("balance_as_of");
                Date closedOn = rs.getDate("closed_on");
                return new Account(
                        rs.getObject("id", UUID.class),
                        fromDb(AccountSource.class, rs.getString("source")),
                        rs.getString("name"),
                        rs.getString("institution_name"),
                        rs.getString("last4"),
                        fromDb(AccountKind.class, rs.getString("kind")),
                        rs.getString("currency"),
                        rs.getBigDecimal("apr"),
                        rs.getBigDecimal("minimum_payment"),
                        fromDb(UpdateReminder.class, rs.getString("update_reminder")),
                        rs.getBoolean("hidden"),
                        closedOn == null ? null : closedOn.toLocalDate(),
                        rs.getBigDecimal("balance"),
                        balanceAsOf == null ? null : balanceAsOf.toLocalDate(),
                        rs.getInt("snapshot_count"));
            };

    private final JdbcClient jdbcClient;

    public AccountRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // Enums are stored as their lowercase names; these two helpers are the only conversion.
    private static String toDb(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static <E extends Enum<E>> E fromDb(Class<E> type, String value) {
        return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    }

    public List<Account> findAll(UUID userId) {
        return jdbcClient
                .sql(SELECT + " order by a.created_at, a.id")
                .param("userId", userId)
                .query(ROW_MAPPER)
                .list();
    }

    public Optional<Account> findById(UUID userId, UUID id) {
        return jdbcClient
                .sql(SELECT + " and a.id = :id")
                .param("userId", userId)
                .param("id", id)
                .query(ROW_MAPPER)
                .optional();
    }

    public UUID insertManual(UUID userId, NewManualAccount account) {
        UUID id = UUID.randomUUID();
        jdbcClient
                .sql(
                        """
                        insert into accounts (id, user_id, source, name, institution_name, kind,
                                              currency, apr, minimum_payment, update_reminder)
                        values (:id, :userId, 'manual', :name, :institutionName, :kind,
                                :currency, :apr, :minimumPayment, :updateReminder)
                        """)
                .param("id", id)
                .param("userId", userId)
                .param("name", account.name())
                .param("institutionName", account.institutionName())
                .param("kind", toDb(account.kind()))
                .param("currency", account.currency())
                .param("apr", account.apr())
                .param("minimumPayment", account.minimumPayment())
                .param("updateReminder", toDb(account.updateReminder()))
                .update();
        return id;
    }

    /** One snapshot per account per day: a second write on the same date replaces the balance. */
    public void upsertSnapshot(UUID userId, UUID accountId, LocalDate date, BigDecimal balance) {
        jdbcClient
                .sql(
                        """
                        insert into balance_snapshots (account_id, user_id, date, balance)
                        select a.id, a.user_id, :date, :balance
                        from accounts a
                        where a.id = :accountId and a.user_id = :userId
                        on conflict (account_id, date) do update set balance = excluded.balance
                        """)
                .param("accountId", accountId)
                .param("userId", userId)
                .param("date", date)
                .param("balance", balance)
                .update();
    }

    public void update(
            UUID userId,
            UUID id,
            String name,
            String institutionName,
            BigDecimal apr,
            BigDecimal minimumPayment,
            UpdateReminder reminder,
            boolean hidden) {
        jdbcClient
                .sql(
                        """
                        update accounts
                        set name = :name, institution_name = :institutionName, apr = :apr,
                            minimum_payment = :minimumPayment, update_reminder = :reminder,
                            hidden = :hidden
                        where id = :id and user_id = :userId
                        """)
                .param("id", id)
                .param("userId", userId)
                .param("name", name)
                .param("institutionName", institutionName)
                .param("apr", apr)
                .param("minimumPayment", minimumPayment)
                .param("reminder", toDb(reminder))
                .param("hidden", hidden)
                .update();
    }

    public void setClosedOn(UUID userId, UUID id, LocalDate closedOnOrNull) {
        jdbcClient
                .sql(
                        "update accounts set closed_on = :closedOn where id = :id and user_id = :userId")
                .param("id", id)
                .param("userId", userId)
                .param("closedOn", closedOnOrNull)
                .update();
    }

    /** Snapshots go with it (on delete cascade). */
    public void delete(UUID userId, UUID id) {
        jdbcClient
                .sql("delete from accounts where id = :id and user_id = :userId")
                .param("id", id)
                .param("userId", userId)
                .update();
    }
}
