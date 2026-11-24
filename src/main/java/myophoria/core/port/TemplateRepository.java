package myophoria.core.port;

import java.util.List;
import myophoria.core.model.WorkoutTemplate;

/** Storage for templates; findById returns null when missing. */
public interface TemplateRepository {
    /** Returns every template with its full exercise list. */
    List<WorkoutTemplate> findAll();

    /** Returns the template with this ID, or null. */
    WorkoutTemplate findById(String id);

    /** Stores a new template and its exercise rows, in one transaction. */
    void insert(WorkoutTemplate template);

    /** Saves the name and replaces the exercise rows, in one transaction. */
    void update(WorkoutTemplate template);

    /** Removes the template and its rows. */
    void delete(String id);
}
