package myophoria.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import myophoria.core.model.WorkoutTemplate;
import myophoria.core.port.TemplateRepository;

/** SQLite implementation of the template port; the exercise list lives in template_exercises by position. */
public class SqliteTemplateRepository implements TemplateRepository {
    private final Database db;

    /** Creates the repository on an opened database. */
    public SqliteTemplateRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<WorkoutTemplate> findAll() {
        return load(null);
    }

    @Override
    public WorkoutTemplate findById(String id) {
        List<WorkoutTemplate> found = load(id);
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public void insert(WorkoutTemplate t) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO workout_templates (id, name) VALUES (?, ?)")) {
                ps.setString(1, t.getId());
                ps.setString(2, t.getName());
                ps.executeUpdate();
            }
            insertRows(c, t);
        }, t.getId());
    }

    @Override
    public void update(WorkoutTemplate t) {
        inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE workout_templates SET name = ? WHERE id = ?");
                    PreparedStatement del = c.prepareStatement("DELETE FROM template_exercises WHERE template_id = ?")) {
                ps.setString(1, t.getName());
                ps.setString(2, t.getId());
                ps.executeUpdate();
                del.setString(1, t.getId());
                del.executeUpdate();
            }
            insertRows(c, t);
        }, t.getId());
    }

    @Override
    public void delete(String id) {
        // template_exercises.template_id has ON DELETE CASCADE
        try (Connection c = db.connect();
                PreparedStatement ps = c.prepareStatement("DELETE FROM workout_templates WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete template " + id, e);
        }
    }

    private static void insertRows(Connection c, WorkoutTemplate t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO template_exercises (template_id, exercise_id, position) VALUES (?, ?, ?)")) {
            int position = 1;
            for (String exerciseId : t.getExerciseIds()) {
                ps.setString(1, t.getId());
                ps.setString(2, exerciseId);
                ps.setInt(3, position++);
                ps.executeUpdate();
            }
        }
    }

    private interface Work {
        void run(Connection c) throws SQLException;
    }

    private void inTransaction(Work work, String id) {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                work.run(c);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save template " + id, e);
        }
    }

    // id == null loads every template
    private List<WorkoutTemplate> load(String id) {
        String where = id == null ? "" : " WHERE id = ?";
        String rowWhere = id == null ? "" : " WHERE template_id = ?";
        try (Connection c = db.connect()) {
            Map<String, List<String>> exerciseIds = new HashMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT template_id, exercise_id FROM template_exercises" + rowWhere + " ORDER BY template_id, position")) {
                if (id != null) {
                    ps.setString(1, id);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        exerciseIds.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
                    }
                }
            }
            List<WorkoutTemplate> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT id, name FROM workout_templates" + where)) {
                if (id != null) {
                    ps.setString(1, id);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String templateId = rs.getString("id");
                        result.add(new WorkoutTemplate(templateId, rs.getString("name"),
                                exerciseIds.getOrDefault(templateId, List.of())));
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load templates", e);
        }
    }
}
