package myophoria.core.csv;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import myophoria.core.error.CorruptedCsvException;

/** RFC 4180 CSV writing and reading, plus the shared timestamp format. */
public final class CsvCodec {
    /** Fixed-width UTC timestamp used by CSV and the database for every Instant. */
    public static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private CsvCodec() {
    }

    /** Joins fields into one CSV line (no line ending); null becomes an empty field. */
    public static String formatRow(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String f = fields[i] == null ? "" : fields[i];
            boolean quote = f.contains(",") || f.contains("\"") || f.contains("\n") || f.contains("\r");
            if (quote) {
                sb.append('"').append(f.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(f);
            }
        }
        return sb.toString();
    }

    /**
     * Reads all rows (header first). Handles quotes, embedded commas and line breaks, a BOM, CRLF or LF, and skips
     * blank lines. Every row must have as many columns as the header.
     */
    public static List<String[]> parse(Reader in, String fileName) throws CorruptedCsvException, IOException {
        List<String[]> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean afterQuote = false; // just closed a quoted field: only , or a line break may follow
        boolean rowHasData = false;
        int line = 1;
        int rowStart = 1;
        int c = in.read();
        if (c == '﻿') {
            c = in.read();
        }
        while (c != -1) {
            if (inQuotes) {
                if (c == '"') {
                    int next = in.read();
                    if (next == '"') {
                        field.append('"');
                    } else {
                        inQuotes = false;
                        afterQuote = true;
                        c = next;
                        continue;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append((char) c);
                }
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                afterQuote = false;
                rowHasData = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r') {
                    int next = in.read();
                    if (next != '\n') {
                        c = next;
                        line++;
                        endRow(rows, fields, field, rowHasData, fileName, rowStart);
                        rowHasData = false;
                        afterQuote = false;
                        rowStart = line;
                        continue;
                    }
                }
                line++;
                endRow(rows, fields, field, rowHasData, fileName, rowStart);
                rowHasData = false;
                afterQuote = false;
                rowStart = line;
            } else if (afterQuote) {
                throw new CorruptedCsvException(fileName, line, "unexpected character after a closing quote");
            } else if (c == '"' && field.length() == 0) {
                inQuotes = true;
                rowHasData = true;
            } else {
                field.append((char) c);
                rowHasData = true;
            }
            c = in.read();
        }
        if (inQuotes) {
            throw new CorruptedCsvException(fileName, rowStart, "unterminated quoted field");
        }
        endRow(rows, fields, field, rowHasData, fileName, rowStart);
        return rows;
    }

    private static void endRow(List<String[]> rows, List<String> fields, StringBuilder field, boolean rowHasData,
            String fileName, int rowStart) throws CorruptedCsvException {
        if (!rowHasData && field.length() == 0 && fields.isEmpty()) {
            return; // blank line or end of file
        }
        fields.add(field.toString());
        field.setLength(0);
        String[] row = fields.toArray(new String[0]);
        fields.clear();
        if (!rows.isEmpty() && row.length != rows.get(0).length) {
            throw new CorruptedCsvException(fileName, rowStart,
                    "expected " + rows.get(0).length + " columns, found " + row.length);
        }
        rows.add(row);
    }

    // ---- field helpers for the entities' fromCsvRow/toCsvRow. They report file "" and line 0;
    // ---- CsvService rethrows with the real file name and line number.

    /** Throws unless the row has exactly n columns. */
    public static void requireColumns(String[] row, int n) throws CorruptedCsvException {
        if (row == null || row.length != n) {
            throw bad("expected " + n + " columns");
        }
    }

    /** Returns the text, or null for an empty field. */
    public static String optional(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /** Parses a decimal number with a '.' point. */
    public static double number(String value, String column) throws CorruptedCsvException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            throw bad(column + " is not a number: " + value);
        }
    }

    /** Parses a decimal number, or null for an empty field. */
    public static Double optionalNumber(String value, String column) throws CorruptedCsvException {
        return optional(value) == null ? null : number(value, column);
    }

    /** Parses a whole number. */
    public static int integer(String value, String column) throws CorruptedCsvException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw bad(column + " is not a whole number: " + value);
        }
    }

    /** Parses "true" or "false" (nothing else). */
    public static boolean bool(String value, String column) throws CorruptedCsvException {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw bad(column + " must be true or false: " + value);
    }

    /** Parses an enum constant by its exact name. */
    public static <E extends Enum<E>> E enumValue(Class<E> type, String value, String column)
            throws CorruptedCsvException {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw bad(column + " has an unknown value: " + value);
        }
    }

    /** Parses a TIMESTAMP field, or null for an empty field. */
    public static Instant instant(String value, String column) throws CorruptedCsvException {
        if (optional(value) == null) {
            return null;
        }
        try {
            return Instant.from(TIMESTAMP.parse(value));
        } catch (DateTimeParseException e) {
            throw bad(column + " is not a timestamp like 2026-11-25T07:30:00.000Z: " + value);
        }
    }

    /** Parses a YYYY-MM-DD date. */
    public static LocalDate date(String value, String column) throws CorruptedCsvException {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw bad(column + " is not a date like 2026-11-25: " + value);
        }
    }

    /** Writes a number without exponent notation. */
    public static String format(double value) {
        return BigDecimal.valueOf(value).toPlainString();
    }

    /** Writes a timestamp, or an empty field for null. */
    public static String format(Instant value) {
        return value == null ? "" : TIMESTAMP.format(value);
    }

    /** Builds the exception for a bad field (file and line are filled in by CsvService). */
    public static CorruptedCsvException bad(String reason) {
        return new CorruptedCsvException("", 0, reason);
    }
}
