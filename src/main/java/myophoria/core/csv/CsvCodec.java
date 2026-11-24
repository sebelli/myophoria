package myophoria.core.csv;

import java.io.IOException;
import java.io.Reader;
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
}
