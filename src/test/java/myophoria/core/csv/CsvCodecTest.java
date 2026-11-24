package myophoria.core.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.List;
import myophoria.core.error.CorruptedCsvException;
import org.junit.jupiter.api.Test;

class CsvCodecTest {
    private static List<String[]> parse(String text) throws Exception {
        return CsvCodec.parse(new StringReader(text), "test.csv");
    }

    @Test
    void formatQuotesOnlyWhenNeeded() {
        assertEquals("a,,c", CsvCodec.formatRow(new String[] {"a", null, "c"}));
        assertEquals("\"a,b\",\"say \"\"hi\"\"\",\"x\ny\"", CsvCodec.formatRow(new String[] {"a,b", "say \"hi\"", "x\ny"}));
        assertEquals("Café Ünïcode", CsvCodec.formatRow(new String[] {"Café Ünïcode"}));
    }

    @Test
    void roundTripOfTrickyValues() throws Exception {
        String[] row = {"plain", "a,b", "quote \"q\"", "line\nbreak", "", "Bänkdrücken 💪", "\r\n"};
        String text = CsvCodec.formatRow(new String[] {"1", "2", "3", "4", "5", "6", "7"}) + "\n"
                + CsvCodec.formatRow(row) + "\n";
        List<String[]> rows = parse(text);
        assertEquals(2, rows.size());
        assertArrayEquals(row, rows.get(1));
    }

    @Test
    void acceptsBomCrlfAndMissingFinalNewline() throws Exception {
        List<String[]> rows = parse("﻿id,name\r\n1,Squat\r\n2,\"Bench, flat\"");
        assertEquals(3, rows.size());
        assertArrayEquals(new String[] {"id", "name"}, rows.get(0));
        assertArrayEquals(new String[] {"2", "Bench, flat"}, rows.get(2));
    }

    @Test
    void skipsBlankLinesAndKeepsEmptyFields() throws Exception {
        List<String[]> rows = parse("a,b\n\n1,\n,2\n\n");
        assertEquals(3, rows.size());
        assertArrayEquals(new String[] {"1", ""}, rows.get(1));
        assertArrayEquals(new String[] {"", "2"}, rows.get(2));
    }

    @Test
    void wrongColumnCountReportsTheLine() {
        CorruptedCsvException e = assertThrows(CorruptedCsvException.class, () -> parse("a,b\n1,2\n\"multi\nline\",2\n1,2,3\n"));
        assertEquals("test.csv", e.getFile());
        assertEquals(5, e.getLine()); // the quoted field spans lines 3-4
        assertTrue(e.getReason().contains("expected 2 columns, found 3"));
    }

    @Test
    void malformedQuotes() {
        CorruptedCsvException open = assertThrows(CorruptedCsvException.class, () -> parse("a\n\"never closed\n"));
        assertEquals(2, open.getLine());
        assertThrows(CorruptedCsvException.class, () -> parse("a,b\n\"x\"y,2\n"));
    }

    @Test
    void emptyInputHasNoRows() throws Exception {
        assertTrue(parse("").isEmpty());
        assertTrue(parse("﻿").isEmpty());
    }
}
