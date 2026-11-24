package myophoria.core.service;

import java.util.List;
import myophoria.core.model.Unit;

/** Suggested rows for an exercise card; warmupWeight and bodyweight may be null. */
public record Prefill(Unit unit, List<PrefillRow> rows, Double warmupWeight, Double bodyweight) {
}
