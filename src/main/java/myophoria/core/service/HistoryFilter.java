package myophoria.core.service;

import java.time.LocalDate;

/** Optional history filters; null means "any". Dates are inclusive. */
public record HistoryFilter(LocalDate from, LocalDate to, String gymId, String exerciseId) {
}
