package dev.felipeflohr.dbeavermcp.util;

import dev.felipeflohr.dbeavermcp.exception.DBeaverMCPValidationException;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NullMarked;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NullMarked
@UtilityClass
public class JdbcUrlUtils {
    public String rewriteJdbcUrlHostAndPort(String jdbcUrl, String newHostname, int newPort) throws DBeaverMCPValidationException {
        JdbcUrlParts urlParts = extractJdbcUrlParts(jdbcUrl);
        return urlParts.prefix() + newHostname + ":" + newPort + urlParts.suffix();
    }

    public JdbcUrlParts extractJdbcUrlParts(String jdbcUrl) throws DBeaverMCPValidationException {
        Pattern pattern = Pattern.compile("(jdbc:[^/]*//|jdbc:oracle:thin:@//)([^:/]+):(\\d+)(.*)");
        Matcher matcher = pattern.matcher(jdbcUrl);
        if (!matcher.matches()) {
            throw new DBeaverMCPValidationException("Cannot parse JDBC URL %s".formatted(jdbcUrl));
        }

        return new JdbcUrlParts(matcher.group(1), matcher.group(2), Integer.parseInt(matcher.group(3)), matcher.group(4));
    }

    public record JdbcUrlParts(String prefix, String host, int port, String suffix) {}
}
