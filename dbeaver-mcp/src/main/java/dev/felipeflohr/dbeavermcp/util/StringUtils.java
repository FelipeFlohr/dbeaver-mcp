package dev.felipeflohr.dbeavermcp.util;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

@NullMarked
@UtilityClass
public class StringUtils {
    public boolean isBlank(@Nullable String val) {
        return val == null || val.isBlank();
    }

    public boolean isNotBlank(@Nullable String val) {
        return !isBlank(val);
    }

    public String getExceptionStackTraceAsString(Throwable t) {
        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
