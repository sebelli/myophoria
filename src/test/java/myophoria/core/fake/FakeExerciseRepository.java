package myophoria.core.fake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import myophoria.core.model.Exercise;
import myophoria.core.port.ExerciseRepository;

/** In-memory ExerciseRepository for service tests; setCount fakes logged sets. */
public class FakeExerciseRepository implements ExerciseRepository {
    private final Map<String, Exercise> byId = new LinkedHashMap<>();
    private final Map<String, Integer> sets = new HashMap<>();

    @Override
    public List<Exercise> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public Exercise findById(String id) {
        return byId.get(id);
    }

    @Override
    public void insert(Exercise e) {
        byId.put(e.getId(), e);
    }

    @Override
    public void update(Exercise e) {
        byId.put(e.getId(), e);
    }

    @Override
    public int countSets(String exerciseId) {
        return sets.getOrDefault(exerciseId, 0);
    }

    @Override
    public int countSessions(String exerciseId) {
        return countSets(exerciseId) > 0 ? 1 : 0;
    }

    @Override
    public void deletePermanently(String id) {
        byId.remove(id);
    }

    /** Pretends the exercise has this many logged sets. */
    public void setCount(String exerciseId, int count) {
        sets.put(exerciseId, count);
    }
}
