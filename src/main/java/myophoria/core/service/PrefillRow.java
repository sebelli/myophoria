package myophoria.core.service;

import myophoria.core.model.SetType;

/** One suggested row on the logging screen, weight already in the prefill unit. */
public record PrefillRow(SetType setType, double weight, int reps) {
}
