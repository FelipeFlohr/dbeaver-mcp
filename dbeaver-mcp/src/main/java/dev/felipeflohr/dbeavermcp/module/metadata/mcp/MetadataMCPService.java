package dev.felipeflohr.dbeavermcp.module.metadata.mcp;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import dev.felipeflohr.dbeavermcp.module.query.factory.QueryServiceFactory;
import dev.felipeflohr.dbeavermcp.module.query.model.StatementResponseDTO;
import dev.felipeflohr.dbeavermcp.module.query.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Metadata browsing tools for Oracle connections. All tools delegate to the existing read-only
 * query path ({@link QueryService#executeReadOnlyStatements}), so they inherit the Oracle
 * {@code SET TRANSACTION READ ONLY} enforcement, CLOB handling and connection resolution.
 * SQL is built from validated identifiers (no bind parameters are available on that path);
 * every statement is a SELECT, so no DDL/DML can be injected.
 */
@Slf4j
@SuppressWarnings("unused")
@NullMarked
@RequiredArgsConstructor
@Service
public class MetadataMCPService {
    private static final Pattern IDENT = Pattern.compile("^[A-Za-z0-9_$#]+$");
    private static final Set<String> DDL_TYPES = Set.of(
            "TABLE", "VIEW", "MATERIALIZED_VIEW", "INDEX", "SEQUENCE", "TRIGGER",
            "PROCEDURE", "FUNCTION", "PACKAGE", "PACKAGE_BODY", "TYPE", "TYPE_BODY",
            "SYNONYM", "CONSTRAINT");

    private final QueryServiceFactory queryServiceFactory;

    @McpTool(
            name = "list_schemas",
            description = "Lists the database schemas/owners visible on a DBeaver connection (Oracle). " +
                    "Use a connection name returned by 'list_available_connections'."
    )
    public List<StatementResponseDTO> listSchemas(
            @McpToolParam(description = "The DBeaver connection name") String connectionName
    ) throws Exception {
        return run(connectionName, List.of("SELECT username AS SCHEMA_NAME FROM all_users ORDER BY username"));
    }

    @McpTool(
            name = "list_tables",
            description = "Lists tables on a connection (Oracle). Optional 'owner' (defaults to the connection's " +
                    "current schema) and optional 'namePattern' (case-insensitive LIKE; % and _ wildcards allowed, " +
                    "wrapped in % automatically if none given)."
    )
    public List<StatementResponseDTO> listTables(
            @McpToolParam(description = "The DBeaver connection name") String connectionName,
            @McpToolParam(description = "Optional schema/owner; defaults to the current schema", required = false) @Nullable String owner,
            @McpToolParam(description = "Optional table name filter (case-insensitive LIKE)", required = false) @Nullable String namePattern
    ) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT owner, table_name FROM all_tables WHERE owner = ")
                .append(ownerExpr(owner));
        if (isSet(namePattern)) sql.append(" AND UPPER(table_name) LIKE ").append(likeLit(namePattern));
        sql.append(" ORDER BY owner, table_name");
        return run(connectionName, List.of(sql.toString()));
    }

    @McpTool(
            name = "describe_table",
            description = "Describes a table (Oracle): returns 3 result sets - (1) columns with data type, length/precision, " +
                    "nullable, default and column comment; (2) PK/FK/unique constraints with columns and referenced table; " +
                    "(3) the table comment."
    )
    public List<StatementResponseDTO> describeTable(
            @McpToolParam(description = "The DBeaver connection name") String connectionName,
            @McpToolParam(description = "Table name") String tableName,
            @McpToolParam(description = "Optional schema/owner; defaults to the current schema", required = false) @Nullable String owner
    ) throws Exception {
        String t = lit(ident(tableName, "tableName"));
        String o = ownerExpr(owner);
        String columns = "SELECT c.column_id, c.column_name, c.data_type, c.data_length, c.data_precision, " +
                "c.data_scale, c.nullable, c.data_default, cc.comments AS column_comment " +
                "FROM all_tab_columns c " +
                "LEFT JOIN all_col_comments cc ON cc.owner = c.owner AND cc.table_name = c.table_name AND cc.column_name = c.column_name " +
                "WHERE c.owner = " + o + " AND c.table_name = " + t + " ORDER BY c.column_id";
        String constraints = "SELECT ac.constraint_type, ac.constraint_name, acc.column_name, acc.position, " +
                "ac.r_owner, ac.r_constraint_name, " +
                "(SELECT rc.table_name FROM all_constraints rc WHERE rc.owner = ac.r_owner AND rc.constraint_name = ac.r_constraint_name) AS referenced_table " +
                "FROM all_constraints ac " +
                "JOIN all_cons_columns acc ON acc.owner = ac.owner AND acc.constraint_name = ac.constraint_name AND acc.table_name = ac.table_name " +
                "WHERE ac.owner = " + o + " AND ac.table_name = " + t + " AND ac.constraint_type IN ('P','R','U') " +
                "ORDER BY ac.constraint_type, ac.constraint_name, acc.position";
        String tableComment = "SELECT comments AS table_comment FROM all_tab_comments WHERE owner = " + o + " AND table_name = " + t;
        return run(connectionName, List.of(columns, constraints, tableComment));
    }

    @McpTool(
            name = "get_ddl",
            description = "Returns the DDL of a database object via DBMS_METADATA.GET_DDL (Oracle). " +
                    "objectType is one of TABLE, VIEW, MATERIALIZED_VIEW, INDEX, SEQUENCE, TRIGGER, PROCEDURE, " +
                    "FUNCTION, PACKAGE, PACKAGE_BODY, TYPE, TYPE_BODY, SYNONYM, CONSTRAINT."
    )
    public List<StatementResponseDTO> getDdl(
            @McpToolParam(description = "The DBeaver connection name") String connectionName,
            @McpToolParam(description = "Object type (e.g. TABLE, VIEW, PACKAGE, PACKAGE_BODY, TRIGGER, FUNCTION, PROCEDURE)") String objectType,
            @McpToolParam(description = "Object name") String objectName,
            @McpToolParam(description = "Optional schema/owner; defaults to the current schema", required = false) @Nullable String owner
    ) throws Exception {
        String type = ident(objectType, "objectType");
        if (!DDL_TYPES.contains(type)) {
            throw new DBeaverMCPValidationException("Unsupported objectType '%s'. Allowed: %s".formatted(objectType, DDL_TYPES));
        }
        String name = lit(ident(objectName, "objectName"));
        String sql = "SELECT DBMS_METADATA.GET_DDL(" + lit(type) + ", " + name + ", " + ownerExpr(owner) + ") AS DDL FROM dual";
        return run(connectionName, List.of(sql));
    }

    @McpTool(
            name = "count_rows",
            description = "Counts rows in a table (Oracle), with an optional WHERE clause. Read-only."
    )
    public List<StatementResponseDTO> countRows(
            @McpToolParam(description = "The DBeaver connection name") String connectionName,
            @McpToolParam(description = "Table name") String tableName,
            @McpToolParam(description = "Optional schema/owner; defaults to the current schema", required = false) @Nullable String owner,
            @McpToolParam(description = "Optional WHERE clause WITHOUT the 'WHERE' keyword, e.g. \"STATO = 'A'\"", required = false) @Nullable String whereClause
    ) throws Exception {
        String t = ident(tableName, "tableName");
        String from = isSet(owner) ? "\"" + ident(owner, "owner") + "\".\"" + t + "\"" : "\"" + t + "\"";
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS ROW_COUNT FROM ").append(from);
        if (isSet(whereClause)) sql.append(" WHERE ").append(whereClause);
        return run(connectionName, List.of(sql.toString()));
    }

    @McpTool(
            name = "search_objects",
            description = "Finds tables and columns whose name matches a pattern (Oracle, case-insensitive LIKE). " +
                    "Returns 2 result sets: matching tables, then matching columns. Optional 'owner' restricts the search."
    )
    public List<StatementResponseDTO> searchObjects(
            @McpToolParam(description = "The DBeaver connection name") String connectionName,
            @McpToolParam(description = "Name pattern (case-insensitive; % and _ wildcards allowed, wrapped in % if none given)") String namePattern,
            @McpToolParam(description = "Optional schema/owner to restrict the search", required = false) @Nullable String owner
    ) throws Exception {
        String like = likeLit(namePattern);
        String ownerClause = isSet(owner) ? " AND owner = " + lit(ident(owner, "owner")) : "";
        String tables = "SELECT owner, table_name FROM all_tables WHERE UPPER(table_name) LIKE " + like + ownerClause +
                " ORDER BY owner, table_name";
        String columns = "SELECT owner, table_name, column_name FROM all_tab_columns WHERE UPPER(column_name) LIKE " + like + ownerClause +
                " ORDER BY owner, table_name, column_name";
        return run(connectionName, List.of(tables, columns));
    }

    private List<StatementResponseDTO> run(String connectionName, List<String> sqls) throws DBeaverMCPValidationException {
        QueryService queryService = queryServiceFactory.getFromConnectionName(connectionName);
        return queryService.executeReadOnlyStatements(sqls, connectionName, 60);
    }

    private static boolean isSet(@Nullable String s) {
        return s != null && !s.isBlank();
    }

    /** Validates a SQL identifier (owner/table/object/type) and upper-cases it. */
    private static String ident(String raw, String what) throws DBeaverMCPValidationException {
        String v = raw == null ? "" : raw.trim();
        if (!IDENT.matcher(v).matches()) {
            throw new DBeaverMCPValidationException("Invalid %s '%s': only letters, digits and _ $ # are allowed.".formatted(what, raw));
        }
        return v.toUpperCase(Locale.ROOT);
    }

    /** Wraps a value as a single-quoted SQL string literal, escaping embedded quotes. */
    private static String lit(String raw) {
        return "'" + raw.replace("'", "''") + "'";
    }

    /** Builds an upper-cased LIKE literal, wrapping in % when the caller supplied no wildcard. */
    private static String likeLit(@Nullable String raw) {
        String v = (raw == null ? "" : raw.trim()).toUpperCase(Locale.ROOT);
        if (!v.contains("%") && !v.contains("_")) v = "%" + v + "%";
        return lit(v);
    }

    /** Owner as a SQL expression: a quoted literal when supplied, otherwise the current schema. */
    private static String ownerExpr(@Nullable String owner) throws DBeaverMCPValidationException {
        if (!isSet(owner)) return "SYS_CONTEXT('USERENV','CURRENT_SCHEMA')";
        return lit(ident(owner, "owner"));
    }
}
