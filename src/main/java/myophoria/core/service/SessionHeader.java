package myophoria.core.service;

import java.time.Instant;
import java.time.LocalDate;

/** One row of the history list. */
public record SessionHeader(String id, String gymName, LocalDate localDate, Instant startedAt, Instant endedAt,
        int setCount, boolean open) {
}
