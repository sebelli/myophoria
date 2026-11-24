package myophoria.core.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import myophoria.core.csv.CsvCodec;
import myophoria.core.csv.CsvSerializable;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.error.InvalidWorkoutInputException;

/** A workout at one gym on one day, holding its completed sets. */
public class WorkoutSession implements VolumeCalculable, CsvSerializable {
    private final String id;
    private String gymId;
    private LocalDate localDate;
    private Instant startedAt; // null for a backfilled session without times
    private Instant endedAt;   // null while open, or for a backfill without an end
    private boolean open;
    private final List<WorkoutSet> sets = new ArrayList<>();

    /** Creates a session; values go through the validated setters. */
    public WorkoutSession(String id, String gymId, LocalDate localDate, Instant startedAt, Instant endedAt,
            boolean open) {
        if (id == null || id.isBlank()) {
            throw new InvalidWorkoutInputException("Session id is required");
        }
        this.id = id;
        setGymId(gymId);
        setLocalDate(localDate);
        this.open = open;
        setTimes(startedAt, endedAt);
    }

    /** Returns the total volume of all sets in the reporting unit. */
    @Override
    public double volume(Unit reportingUnit) {
        double total = 0;
        for (WorkoutSet set : sets) {
            total += set.volume(reportingUnit);
        }
        return total;
    }

    /** Closes the session at the given time. */
    public void finish(Instant now) {
        if (!open) {
            throw new InvalidWorkoutInputException("The workout is already finished");
        }
        if (now == null || (startedAt != null && now.isBefore(startedAt))) {
            throw new InvalidWorkoutInputException("End time cannot be before start time");
        }
        open = false;
        endedAt = now;
    }

    /** Returns the end time, else the last set's completion time, else null. */
    public Instant effectiveEnd() {
        if (endedAt != null) {
            return endedAt;
        }
        Instant last = null;
        for (WorkoutSet set : sets) {
            if (last == null || set.getCompletedAt().isAfter(last)) {
                last = set.getCompletedAt();
            }
        }
        return last;
    }

    /** Returns the duration, or null unless both start and effective end exist. */
    public Duration duration() {
        Instant end = effectiveEnd();
        return startedAt == null || end == null ? null : Duration.between(startedAt, end);
    }

    /** Attaches a loaded set (used by repositories; the set must belong to this session). */
    public void addSet(WorkoutSet set) {
        if (!set.getSessionId().equals(id)) {
            throw new IllegalArgumentException("Set " + set.getId() + " belongs to another session");
        }
        sets.add(set);
    }

    /** Returns the sets as a read-only list. */
    public List<WorkoutSet> getSets() {
        return Collections.unmodifiableList(sets);
    }

    /** Returns the unique ID. */
    public String getId() {
        return id;
    }

    /** Returns the gym of this session. */
    public String getGymId() {
        return gymId;
    }

    /** Returns the calendar day as the client saw it. */
    public LocalDate getLocalDate() {
        return localDate;
    }

    /** Returns the start time, or null. */
    public Instant getStartedAt() {
        return startedAt;
    }

    /** Returns the end time, or null. */
    public Instant getEndedAt() {
        return endedAt;
    }

    /** Returns true while the workout is running. */
    public boolean isOpen() {
        return open;
    }

    /** Sets the gym; it must not be blank. */
    public void setGymId(String gymId) {
        if (gymId == null || gymId.isBlank()) {
            throw new InvalidWorkoutInputException("Gym is required");
        }
        this.gymId = gymId;
    }

    /** Sets the local date; it must not be null. */
    public void setLocalDate(LocalDate localDate) {
        if (localDate == null) {
            throw new InvalidWorkoutInputException("Local date is required");
        }
        this.localDate = localDate;
    }

    /** Sets start and end together; the end cannot be before the start, and an open session has no end. */
    public void setTimes(Instant startedAt, Instant endedAt) {
        if (open && endedAt != null) {
            throw new InvalidWorkoutInputException("An open workout has no end time");
        }
        if (startedAt != null && endedAt != null && endedAt.isBefore(startedAt)) {
            throw new InvalidWorkoutInputException("End time cannot be before start time");
        }
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    /** Column names of this entity's CSV file. */
    public static final String[] HEADER = {"id", "gym_id", "local_date", "started_at", "ended_at", "is_open"};

    @Override
    public String[] toCsvRow() {
        return new String[] {id, gymId, localDate.toString(), CsvCodec.format(startedAt), CsvCodec.format(endedAt), String.valueOf(open)};
    }

    /** Parses one CSV row in HEADER order; a bad value throws CorruptedCsvException. */
    public static WorkoutSession fromCsvRow(String[] row) throws CorruptedCsvException {
        CsvCodec.requireColumns(row, HEADER.length);
        try {
            return new WorkoutSession(row[0], row[1], CsvCodec.date(row[2], "local_date"),
                    CsvCodec.instant(row[3], "started_at"), CsvCodec.instant(row[4], "ended_at"),
                    CsvCodec.bool(row[5], "is_open"));
        } catch (InvalidWorkoutInputException e) {
            throw CsvCodec.bad(e.getMessage());
        }
    }
}
