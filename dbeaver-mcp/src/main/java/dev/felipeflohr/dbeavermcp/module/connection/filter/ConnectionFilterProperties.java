package dev.felipeflohr.dbeavermcp.module.connection.filter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-instance allow/deny lists that limit which DBeaver connections this MCP instance exposes.
 * Configure via args or env, e.g.
 * {@code --dbeavermcp.connections.include=^prod,^staging} and/or
 * {@code --dbeavermcp.connections.exclude=legacy}.
 * Each entry is a regex matched (case-insensitive, substring via {@code find()}) against the
 * connection NAME. Empty {@code include} means all connections are allowed.
 * <p>Note: commas separate list entries, so avoid {@code {m,n}} quantifiers in a comma-joined
 * value (use the indexed form {@code include[0]=...} if you need them).
 */
@Component
@ConfigurationProperties(prefix = "dbeavermcp.connections")
@Data
public class ConnectionFilterProperties {
    private List<String> include = new ArrayList<>();
    private List<String> exclude = new ArrayList<>();
}
