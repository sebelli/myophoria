package myophoria.core.error;

/** Thrown when a value breaks a domain rule (blank name, negative weight, zero reps). */
public class InvalidWorkoutInputException extends RuntimeException {
    /** Creates the exception with a user-readable reason. */
    public InvalidWorkoutInputException(String message) {
        super(message);
    }
}
