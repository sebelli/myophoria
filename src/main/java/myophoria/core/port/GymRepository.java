package myophoria.core.port;

import java.util.List;
import myophoria.core.model.Gym;

/** Storage for gyms; findById returns null when missing. */
public interface GymRepository {
    /** Returns every gym, active and soft-deleted. */
    List<Gym> findAll();

    /** Returns the gym with this ID, or null. */
    Gym findById(String id);

    /** Stores a new gym. */
    void insert(Gym gym);

    /** Saves changes to an existing gym. */
    void update(Gym gym);

    /** Makes this gym the only default, in one transaction. */
    void makeDefault(String id);
}
