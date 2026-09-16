package com.campaignorganizer.config.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Injects {@code SET app.current_account_id} immediately before every new
 * {@code Statement}/{@code PreparedStatement}/{@code CallableStatement} is
 * created on this connection (ADR-0114) — session-scoped {@code SET}, not
 * transaction-scoped {@code SET LOCAL}, and re-issued before literally every
 * statement, so the value in effect for any given query is always whatever
 * this handler set immediately beforehand, regardless of what transaction
 * (if any) is active at that moment.
 *
 * <p>Two things empirically ruled out simpler designs, both confirmed
 * directly against a running Postgres rather than assumed:
 * <ul>
 * <li>Hooking {@code setAutoCommit(false)} (this class's original design,
 * matching "transaction begins" — chosen to sidestep the {@code @Aspect}/
 * {@code TransactionInterceptor} ordering risk described in the ADR's
 * "Alternatives considered") is unreliable: on a pooled connection,
 * Hibernate does not call {@code setAutoCommit(false)} before every logical
 * transaction it demarcates via {@code commit()} on an already-non-autocommit
 * connection, and JDBC/Hibernate don't guarantee statement <em>preparation</em>
 * happens after the connection's autocommit state is in its final form for
 * that query either — so there's no single connection-level event reliably
 * bracketing "about to run one query" the way a per-statement hook does.</li>
 * <li>Using transaction-scoped {@code SET LOCAL} here instead of {@code SET}
 * would still fail whenever the connection is in autocommit mode: each
 * statement is then its own implicit one-statement transaction, so a
 * {@code SET LOCAL} issued as one JDBC call and a query issued as the next
 * JDBC call can never share a transaction scope, no matter how close
 * together they're issued. A custom GUC that's ever been {@code SET LOCAL}
 * on a session also doesn't revert to SQL {@code NULL} once that scope
 * ends — it reads back as an empty string, and casting {@code ''::uuid}
 * throws outright rather than just failing an equality check, so this
 * wasn't a "some rows leak" bug, it was "some requests 500."</li>
 * </ul>
 * Session-scoped {@code SET}, re-applied before every statement, sidesteps
 * both: it takes effect immediately regardless of transaction/autocommit
 * state, and staleness is a non-issue because it's never read without first
 * being freshly written by the very next statement on this connection.
 */
final class GucSettingConnectionHandler implements InvocationHandler {

    private static final UUID NO_ACCOUNT_SENTINEL = new UUID(0L, 0L);
    private static final Set<String> STATEMENT_FACTORY_METHODS =
            Set.of("createStatement", "prepareStatement", "prepareCall");

    private final Connection delegate;

    GucSettingConnectionHandler(Connection delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (STATEMENT_FACTORY_METHODS.contains(method.getName())) {
                setGuc();
            }
            return method.invoke(delegate, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private void setGuc() throws java.sql.SQLException {
        UUID accountId = currentAccountIdOrSentinel();
        try (Statement statement = delegate.createStatement()) {
            statement.execute("SET app.current_account_id = '" + accountId + "'");
        }
    }

    private UUID currentAccountIdOrSentinel() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID accountId) {
            return accountId;
        }
        return NO_ACCOUNT_SENTINEL;
    }
}
