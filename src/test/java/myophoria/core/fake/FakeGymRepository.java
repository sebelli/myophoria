package myophoria.core.fake;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import myophoria.core.model.Gym;
import myophoria.core.port.GymRepository;

/** In-memory GymRepository, seeded with the default gym like the real migration. */
public class FakeGymRepository implements GymRepository {
    private final Map<String, Gym> byId = new LinkedHashMap<>();

    /** Creates the fake with the seeded "Default" gym. */
    public FakeGymRepository() {
        insert(new Gym(Gym.SEEDED_DEFAULT_ID, "Default", true, null));
    }

    @Override
    public List<Gym> findAll() {
        List<Gym> copies = new ArrayList<>();
        for (Gym g : byId.values()) {
            copies.add(copy(g));
        }
        return copies;
    }

    @Override
    public Gym findById(String id) {
        Gym g = byId.get(id);
        return g == null ? null : copy(g);
    }

    @Override
    public void insert(Gym g) {
        byId.put(g.getId(), copy(g));
    }

    @Override
    public void update(Gym g) {
        byId.put(g.getId(), copy(g));
    }

    @Override
    public void makeDefault(String id) {
        for (Gym g : byId.values()) {
            g.setDefault(g.getId().equals(id));
        }
    }

    private static Gym copy(Gym g) {
        return new Gym(g.getId(), g.getName(), g.isDefault(), g.getDeletedAt());
    }
}
