package myophoria.core.port;

import java.util.List;
import myophoria.core.model.BodyweightEntry;

/** Storage for bodyweight entries; findById returns null when missing. */
public interface BodyweightRepository {
    /** Returns every entry. */
    List<BodyweightEntry> findAll();

    /** Returns the entry with this ID, or null. */
    BodyweightEntry findById(String id);

    /** Stores a new entry. */
    void insert(BodyweightEntry entry);

    /** Saves changes to an existing entry. */
    void update(BodyweightEntry entry);

    /** Removes the entry. */
    void delete(String id);
}
