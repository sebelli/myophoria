package myophoria.core.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import myophoria.core.error.InvalidWorkoutInputException;
import myophoria.core.error.NotFoundException;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.Unit;
import myophoria.core.port.BodyweightRepository;

/** Use cases for bodyweight entries. */
public class BodyweightService {
    private static final String RESOURCE = "bodyweight";
    /** Newest measurement first. */
    public static final Comparator<BodyweightEntry> NEWEST_FIRST =
            Comparator.comparing(BodyweightEntry::getRecordedAt).reversed();

    private final BodyweightRepository entries;

    /** Creates the service on a repository. */
    public BodyweightService(BodyweightRepository entries) {
        this.entries = entries;
    }

    /** Lists all entries, newest first. */
    public List<BodyweightEntry> list() {
        List<BodyweightEntry> all = entries.findAll();
        all.sort(NEWEST_FIRST);
        return all;
    }

    /** Returns the newest entry, or null if there is none. */
    public BodyweightEntry latest() {
        List<BodyweightEntry> all = list();
        return all.isEmpty() ? null : all.get(0);
    }

    /** Records a new measurement. */
    public BodyweightEntry create(double weight, Unit unit, Instant recordedAt, LocalDate localDate) {
        BodyweightEntry e = new BodyweightEntry(UUID.randomUUID().toString(), weight, unit, millis(recordedAt), localDate);
        entries.insert(e);
        return e;
    }

    /** Corrects an existing measurement. */
    public BodyweightEntry update(String id, double weight, Unit unit, Instant recordedAt, LocalDate localDate) {
        BodyweightEntry e = get(id);
        e.setWeight(weight);
        e.setUnit(unit);
        e.setRecordedAt(millis(recordedAt));
        e.setLocalDate(localDate);
        entries.update(e);
        return e;
    }

    /** Deletes a measurement (hard delete: a correction). */
    public void delete(String id) {
        get(id);
        entries.delete(id);
    }

    private BodyweightEntry get(String id) {
        BodyweightEntry e = entries.findById(id);
        if (e == null) {
            throw new NotFoundException(RESOURCE, id);
        }
        return e;
    }

    private static Instant millis(Instant at) {
        if (at == null) {
            throw new InvalidWorkoutInputException("Recorded time is required");
        }
        return at.truncatedTo(ChronoUnit.MILLIS);
    }
}
