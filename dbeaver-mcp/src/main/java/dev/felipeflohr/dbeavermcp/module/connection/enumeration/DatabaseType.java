package dev.felipeflohr.dbeavermcp.module.connection.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Getter
@RequiredArgsConstructor
public enum DatabaseType {
    POSTGRES("postgresql", "org.postgresql.Driver"),
    ORACLE("oracle", "oracle.jdbc.OracleDriver"),
    FIREBIRD("jaybird", "org.firebirdsql.jdbc.FBDriver");

    private final String provider;
    private final String driverClassName;
}
