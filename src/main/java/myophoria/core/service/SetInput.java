package myophoria.core.service;

import myophoria.core.model.SetType;
import myophoria.core.model.Unit;

/** Values for adding or editing a set; bodyweight is null unless the exercise requires it. */
public record SetInput(String exerciseId, SetType setType, double weight, Unit unit, int reps, Double bodyweight,
        int position) {
}
