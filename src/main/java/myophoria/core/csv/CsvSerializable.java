package myophoria.core.csv;

/** An entity that can write itself as one CSV row (each also has a static HEADER and fromCsvRow). */
public interface CsvSerializable {
    /** Returns the fields of this entity in HEADER order. */
    String[] toCsvRow();
}
