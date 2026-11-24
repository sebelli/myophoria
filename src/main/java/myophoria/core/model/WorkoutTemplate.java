package myophoria.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import myophoria.core.csv.CsvCodec;
import myophoria.core.csv.CsvSerializable;
import myophoria.core.error.CorruptedCsvException;
import myophoria.core.error.InvalidWorkoutInputException;

/** A named, ordered list of exercises to start a workout from (no stored set counts). */
public class WorkoutTemplate implements CsvSerializable {
    private final String id;
    private String name;
    private List<String> exerciseIds = new ArrayList<>();

    /** Creates a template; values go through the validated setters. */
    public WorkoutTemplate(String id, String name, List<String> exerciseIds) {
        if (id == null || id.isBlank()) {
            throw new InvalidWorkoutInputException("Template id is required");
        }
        this.id = id;
        setName(name);
        setExerciseIds(exerciseIds);
    }

    /** Returns the unique ID. */
    public String getId() {
        return id;
    }

    /** Returns the name. */
    public String getName() {
        return name;
    }

    /** Returns the exercise IDs in order, read-only. */
    public List<String> getExerciseIds() {
        return Collections.unmodifiableList(exerciseIds);
    }

    /** Sets the name; it must not be blank. */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidWorkoutInputException("Template name is required");
        }
        this.name = name.trim();
    }

    /** Replaces the exercise list; no blanks and no exercise twice. */
    public void setExerciseIds(List<String> exerciseIds) {
        if (exerciseIds == null) {
            throw new InvalidWorkoutInputException("Exercise list is required");
        }
        List<String> copy = new ArrayList<>();
        for (String exerciseId : exerciseIds) {
            if (exerciseId == null || exerciseId.isBlank()) {
                throw new InvalidWorkoutInputException("Exercise id is required");
            }
            if (copy.contains(exerciseId)) {
                throw new InvalidWorkoutInputException("An exercise can appear only once in a template");
            }
            copy.add(exerciseId);
        }
        this.exerciseIds = copy;
    }

    /** Column names of this entity's CSV file. */
    public static final String[] HEADER = {"id", "name", "exercise_ids"};

    @Override
    public String[] toCsvRow() {
        return new String[] {id, name, String.join(";", exerciseIds)};
    }

    /** Parses one CSV row in HEADER order; a bad value throws CorruptedCsvException. */
    public static WorkoutTemplate fromCsvRow(String[] row) throws CorruptedCsvException {
        CsvCodec.requireColumns(row, HEADER.length);
        try {
            List<String> ids = new ArrayList<>();
            if (!row[2].isEmpty()) {
                for (String exerciseId : row[2].split(";", -1)) {
                    ids.add(exerciseId);
                }
            }
            return new WorkoutTemplate(row[0], row[1], ids);
        } catch (InvalidWorkoutInputException e) {
            throw CsvCodec.bad(e.getMessage());
        }
    }
}
