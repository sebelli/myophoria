package myophoria.core.fake;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.port.BodyweightRepository;

/** In-memory BodyweightRepository for service tests. */
public class FakeBodyweightRepository implements BodyweightRepository {
    private final Map<String, BodyweightEntry> byId = new LinkedHashMap<>();

    @Override
    public List<BodyweightEntry> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public BodyweightEntry findById(String id) {
        return byId.get(id);
    }

    @Override
    public void insert(BodyweightEntry e) {
        byId.put(e.getId(), e);
    }

    @Override
    public void update(BodyweightEntry e) {
        byId.put(e.getId(), e);
    }

    @Override
    public void delete(String id) {
        byId.remove(id);
    }
}
