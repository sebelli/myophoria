package myophoria.core.service;

import java.time.LocalDate;

/** Heaviest working set of an exercise in one session. */
public record ProgressPoint(LocalDate localDate, double topWeight) {
}
