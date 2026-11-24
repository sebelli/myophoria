package myophoria.core.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import myophoria.core.model.Unit;

/** Summary of one session in a reporting unit; endedAt is the effective end, durationSeconds may be null. */
public record SessionSummary(String sessionId, LocalDate localDate, Instant startedAt, Instant endedAt,
        Long durationSeconds, Unit unit, int totalSets, int workingSets, double volume,
        List<ExerciseSummary> exercises) {
}
