package myophoria.core.fake;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import myophoria.core.model.WorkoutTemplate;
import myophoria.core.port.TemplateRepository;

/** In-memory TemplateRepository returning copies. */
public class FakeTemplateRepository implements TemplateRepository {
    private final Map<String, WorkoutTemplate> byId = new LinkedHashMap<>();

    @Override
    public List<WorkoutTemplate> findAll() {
        List<WorkoutTemplate> result = new ArrayList<>();
        for (WorkoutTemplate t : byId.values()) {
            result.add(copy(t));
        }
        return result;
    }

    @Override
    public WorkoutTemplate findById(String id) {
        WorkoutTemplate t = byId.get(id);
        return t == null ? null : copy(t);
    }

    @Override
    public void insert(WorkoutTemplate t) {
        byId.put(t.getId(), copy(t));
    }

    @Override
    public void update(WorkoutTemplate t) {
        byId.put(t.getId(), copy(t));
    }

    @Override
    public void delete(String id) {
        byId.remove(id);
    }

    private static WorkoutTemplate copy(WorkoutTemplate t) {
        return new WorkoutTemplate(t.getId(), t.getName(), t.getExerciseIds());
    }
}
