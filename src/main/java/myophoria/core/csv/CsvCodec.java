package myophoria.core.csv;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** CSV helpers; for now only the shared timestamp format (row parsing arrives in T4.1). */
public final class CsvCodec {
    /** Fixed-width UTC timestamp used by CSV and the database for every Instant. */
    public static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private CsvCodec() {
    }
}
