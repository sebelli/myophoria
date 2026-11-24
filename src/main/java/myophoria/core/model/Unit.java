package myophoria.core.model;

import java.util.Locale;

/** A weight unit with its size in kilograms. */
public enum Unit {
    KG(1.0), LB(0.45359237);

    private final double kilograms;

    Unit(double kilograms) {
        this.kilograms = kilograms;
    }

    /** Converts a value in this unit into the target unit. */
    public double convert(double value, Unit to) {
        return value * kilograms / to.kilograms;
    }

    /** Returns the lower-case label used in display text ("kg", "lb"). */
    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
