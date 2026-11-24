package myophoria.core.service;

/** One exercise in a session summary; top and average weight are null without working sets. */
public record ExerciseSummary(String exerciseId, String name, int workingSets, Double topWeight,
        Double averageWeight, double volume) {
}
