package myophoria.core.error;

/** Thrown when a CSV file has a malformed row, header or format version (checked: the importer can report it). */
public class CorruptedCsvException extends Exception {
    private final String file;
    private final int line;
    private final String reason;

    /** Creates the exception for a problem in one CSV file at one line. */
    public CorruptedCsvException(String file, int line, String reason) {
        super(file + " line " + line + ": " + reason);
        this.file = file;
        this.line = line;
        this.reason = reason;
    }

    /** Returns the CSV file name. */
    public String getFile() {
        return file;
    }

    /** Returns the 1-based line number. */
    public int getLine() {
        return line;
    }

    /** Returns the reason the row was rejected. */
    public String getReason() {
        return reason;
    }
}
