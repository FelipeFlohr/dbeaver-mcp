package dev.felipeflohr.dbeavermcp.util;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@UtilityClass
public class StringUtils {
    public boolean isBlank(@Nullable String val) {
        return val == null || val.isBlank();
    }

    public boolean isNotBlank(@Nullable String val) {
        return !isBlank(val);
    }
}
