package dev.felipeflohr.dbeavermcp.test;

import oracle.sql.TIMESTAMP;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@NullMarked
public class AssertionUtil {
    private AssertionUtil() {}

    public static void assertNumber(int expected, Object actual) {
        switch (actual) {
            case BigDecimal bigDecimal -> assertEquals(BigDecimal.valueOf(expected, bigDecimal.scale()), bigDecimal);
            case Integer integer -> assertEquals(expected, integer);
            case Long longValue -> assertEquals(Long.valueOf(expected), longValue);
            default -> throw new AssertionError("Type %s is not a supported number".formatted(actual.getClass().getName()));
        }
    }

    public static void assertDate(LocalDate expected, Object actual) {
        switch (actual) {
            case LocalDate localDate -> assertEquals(expected, localDate);
            case Date date -> assertEquals(expected, date.toLocalDate());
            case Timestamp timestamp -> assertEquals(expected, timestamp.toLocalDateTime().toLocalDate());
            default -> throw new AssertionError("Type %s is not a supported date".formatted(actual.getClass().getName()));
        }
    }

    public static void assertDateTime(LocalDateTime expected, Object actual) throws SQLException {
        switch (actual) {
            case LocalDateTime localDateTime -> assertEquals(expected, localDateTime);
            case Timestamp sqlTimestamp -> assertEquals(expected, sqlTimestamp.toLocalDateTime());
            case TIMESTAMP oracleTimestamp -> assertEquals(expected, oracleTimestamp.toLocalDateTime());
            default -> throw new AssertionError("Type %s is not a supported date".formatted(actual.getClass().getName()));
        }
    }

    public static void assertTrue(Object actual) {
        assertBoolean(true, actual);
    }

    public static void assertFalse(Object actual) {
        assertBoolean(false, actual);
    }

    private static void assertBoolean(boolean expected, Object actual) {
        switch (actual) {
            case Boolean booleanValue -> assertEquals(expected, booleanValue);
            case BigDecimal bigDecimal -> {
                boolean isZero = BigDecimal.ZERO.compareTo(bigDecimal) == 0;
                boolean isOne = BigDecimal.ONE.compareTo(bigDecimal) == 0;
                if (!isZero && !isOne) {
                    throw new AssertionError("Value %s is not a supported BigDecimal boolean".formatted(bigDecimal));
                }
                if (expected) {
                    Assertions.assertTrue(isOne);
                    return;
                }
                Assertions.assertTrue(isZero);
            }
            default -> throw new AssertionError("Type %s is not a supported boolean".formatted(actual.getClass().getName()));
        }
    }
}
