package dev.felipeflohr.dbeavermcp.module.connection.filter;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Decides whether a DBeaver connection is accessible from this MCP instance, based on the
 * include/exclude regex lists in {@link ConnectionFilterProperties}. Used both when listing
 * connections and when resolving a connection for a query, so a hidden connection cannot be
 * reached even by naming it directly.
 */
@Slf4j
@NullMarked
@Service
public class ConnectionFilter {
    private final List<Pattern> include;
    private final List<Pattern> exclude;

    public ConnectionFilter(ConnectionFilterProperties properties) {
        this.include = compile(properties.getInclude());
        this.exclude = compile(properties.getExclude());
        if (!include.isEmpty() || !exclude.isEmpty()) {
            log.info("Connection filter active. include={} exclude={}", properties.getInclude(), properties.getExclude());
        }
    }

    public boolean isAllowed(@Nullable String connectionName) {
        if (connectionName == null) return false;
        boolean included = include.isEmpty() || include.stream().anyMatch(p -> p.matcher(connectionName).find());
        if (!included) return false;
        return exclude.stream().noneMatch(p -> p.matcher(connectionName).find());
    }

    private static List<Pattern> compile(@Nullable List<String> regexes) {
        List<Pattern> compiled = new ArrayList<>();
        if (regexes == null) return compiled;
        for (String regex : regexes) {
            if (regex == null || regex.isBlank()) continue;
            compiled.add(Pattern.compile(regex.trim(), Pattern.CASE_INSENSITIVE));
        }
        return compiled;
    }
}
