package myophoria.core.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Gym;
import myophoria.core.model.Laterality;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.model.WorkoutTemplate;
import org.junit.jupiter.api.Test;

class CsvSerializationTest {
    static final Instant T = Instant.parse("2026-11-25T07:30:00.123Z");
    static final LocalDate DAY = LocalDate.of(2026, 11, 25);

    /** Writes the row as CSV text and reads it back, like export followed by import. */
    private static String[] throughCsv(String[] header, String[] row) throws Exception {
        String text = CsvCodec.formatRow(header) + "\n" + CsvCodec.formatRow(row) + "\n";
        List<String[]> rows = CsvCodec.parse(new StringReader(text), "x.csv");
        assertArrayEquals(header, rows.get(0));
        return rows.get(1);
    }

    @Test
    void exerciseRoundTripKeepsSubclass() throws Exception {
        Exercise e = Exercise.of(EquipmentType.CALISTHENICS, "e1", "Pull-Up, \"weighted\"", Laterality.UNILATERAL, T);
        Exercise back = Exercise.fromCsvRow(throughCsv(Exercise.HEADER, e.toCsvRow()));
        assertEquals(EquipmentType.CALISTHENICS, back.type());
        assertEquals(e.getName(), back.getName());
        assertEquals(Laterality.UNILATERAL, back.getLaterality());
        assertEquals(T, back.getDeletedAt());
        assertNull(Exercise.fromCsvRow(new String[] {"e2", "Squat", "BARBELL", "BILATERAL", ""}).getDeletedAt());
    }

    @Test
    void gymRoundTrip() throws Exception {
        Gym g = new Gym("g1", "Home Gym", true, null);
        Gym back = Gym.fromCsvRow(throughCsv(Gym.HEADER, g.toCsvRow()));
        assertEquals("Home Gym", back.getName());
        assertTrue(back.isDefault());
        assertNull(back.getDeletedAt());
        assertArrayEquals(new String[] {"g1", "Home Gym", "true", ""}, g.toCsvRow());
    }

    @Test
    void sessionAndSetRoundTrip() throws Exception {
        WorkoutSession s = new WorkoutSession("s1", "g1", DAY, T, T.plusSeconds(3600), false);
        WorkoutSession sb = WorkoutSession.fromCsvRow(throughCsv(WorkoutSession.HEADER, s.toCsvRow()));
        assertEquals(DAY, sb.getLocalDate());
        assertEquals(T.plusSeconds(3600), sb.getEndedAt());
        assertFalse(sb.isOpen());

        WorkoutSet set = new WorkoutSet("x1", "s1", "e1", 2, SetType.FAILURE, 22.5, Unit.LB, 6, 154.25, T);
        WorkoutSet xb = WorkoutSet.fromCsvRow(throughCsv(WorkoutSet.HEADER, set.toCsvRow()));
        assertEquals(22.5, xb.getWeight());
        assertEquals(154.25, xb.getBodyweight());
        assertEquals(SetType.FAILURE, xb.getSetType());
        assertEquals(Unit.LB, xb.getUnit());
        assertEquals(2, xb.getPosition());
        assertEquals("22.5", set.toCsvRow()[5]);
        WorkoutSet noBody = new WorkoutSet("x2", "s1", "e1", 1, SetType.WORKING, 100, Unit.KG, 5, null, T);
        assertEquals("", noBody.toCsvRow()[8]);
        assertNull(WorkoutSet.fromCsvRow(noBody.toCsvRow()).getBodyweight());
    }

    @Test
    void bodyweightAndTemplateRoundTrip() throws Exception {
        BodyweightEntry b = new BodyweightEntry("b1", 70.4, Unit.KG, T, DAY);
        BodyweightEntry bb = BodyweightEntry.fromCsvRow(throughCsv(BodyweightEntry.HEADER, b.toCsvRow()));
        assertEquals(70.4, bb.getWeight());
        assertEquals(T, bb.getRecordedAt());

        WorkoutTemplate t = new WorkoutTemplate("t1", "Legs", List.of("e2", "e1"));
        assertEquals("e2;e1", t.toCsvRow()[2]);
        assertEquals(List.of("e2", "e1"), WorkoutTemplate.fromCsvRow(throughCsv(WorkoutTemplate.HEADER, t.toCsvRow())).getExerciseIds());
        assertEquals(List.of(), WorkoutTemplate.fromCsvRow(new String[] {"t2", "Empty", ""}).getExerciseIds());
    }

    @Test
    void badValuesAreCorruptedCsv() {
        String[][] badSets = {
            {"x", "s", "e", "one", "WORKING", "1", "KG", "1", "", "2026-11-25T07:30:00.123Z"},
            {"x", "s", "e", "1", "HEAVY", "1", "KG", "1", "", "2026-11-25T07:30:00.123Z"},
            {"x", "s", "e", "1", "WORKING", "1,5", "KG", "1", "", "2026-11-25T07:30:00.123Z"},
            {"x", "s", "e", "1", "WORKING", "1", "KG", "1", "", "2026-11-25 07:30"},
            {"x", "s", "e", "1", "WORKING", "-1", "KG", "1", "", "2026-11-25T07:30:00.123Z"}, // entity rule
            {"x", "s", "e", "1", "WORKING", "1", "KG", "0", "", "2026-11-25T07:30:00.123Z"},
            {"x", "s", "e", "1", "WORKING", "1", "KG"}};
        for (String[] row : badSets) {
            assertThrows(CorruptedCsvException.class, () -> WorkoutSet.fromCsvRow(row), String.join("|", row));
        }
        assertThrows(CorruptedCsvException.class, () -> Gym.fromCsvRow(new String[] {"g", "G", "yes", ""}));
        assertThrows(CorruptedCsvException.class, () -> Gym.fromCsvRow(new String[] {"g", "", "true", ""}));
        assertThrows(CorruptedCsvException.class, () -> WorkoutSession.fromCsvRow(new String[] {"s", "g", "25.11.2026", "", "", "false"}));
        assertThrows(CorruptedCsvException.class,
                () -> WorkoutSession.fromCsvRow(new String[] {"s", "g", "2026-11-25", "", "2026-11-25T07:30:00.123Z", "true"}));
        assertThrows(CorruptedCsvException.class, () -> Exercise.fromCsvRow(new String[] {"e", "X", "KETTLEBELL", "BILATERAL", ""}));
        assertThrows(CorruptedCsvException.class, () -> BodyweightEntry.fromCsvRow(new String[] {"b", "0", "KG", "2026-11-25T07:30:00.123Z", "2026-11-25"}));
        assertThrows(CorruptedCsvException.class, () -> WorkoutTemplate.fromCsvRow(new String[] {"t", "T", "a;a"}));
    }
}
