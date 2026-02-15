package dev.felipeflohr.dbeavermcp.module.connection.datasource;

import org.jspecify.annotations.NullMarked;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

@NullMarked
public class DBeaverMCPDatasource extends SimpleDriverDataSource {
    public DBeaverMCPDatasource(String driverClassName, String url, String username, String password) throws ClassNotFoundException {
        super();
        setDriverClass(Class.forName(driverClassName).asSubclass(Driver.class));
        setUrl(url);
        setUsername(username);
        setPassword(password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyManualTransactionAndReadOnly(connection);
        return connection;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyManualTransactionAndReadOnly(connection);
        return connection;
    }

    private void applyManualTransactionAndReadOnly(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        connection.setReadOnly(true);
    }
}
