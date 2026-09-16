package com.campaignorganizer.config.db;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Wraps the app's own {@code dataSource} bean (ADR-0114) so every connection
 * it hands out carries the {@code SET LOCAL app.current_account_id} behavior
 * — see {@link GucSettingConnectionHandler}. Delegates connection pooling
 * itself entirely to the wrapped {@link DataSource} (Hikari, built and sized
 * by Spring Boot's own autoconfiguration, untouched); this class only
 * intercepts the {@link Connection} objects that pool already produces.
 */
public final class GucSettingDataSource extends DelegatingDataSource {

    public GucSettingDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return wrap(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return wrap(super.getConnection(username, password));
    }

    private Connection wrap(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                new GucSettingConnectionHandler(real));
    }
}
