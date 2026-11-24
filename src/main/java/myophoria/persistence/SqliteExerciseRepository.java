package myophoria.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import myophoria.core.model.EquipmentType;
import myophoria.core.model.Exercise;
import myophoria.core.model.Laterality;
import myophoria.core.port.ExerciseRepository;

/** SQLite implementation of the exercise port. */
public class SqliteExerciseRepository implements ExerciseRepository {
    private static final String COLUMNS = "id, name, equipment_type, laterality, deleted_at";
    private final Database db;

    /** Creates the repository on an opened database. */
    public SqliteExerciseRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<Exercise> findAll() {
        try (Connection c = db.connect();
                PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM exercises");
                ResultSet rs = ps.executeQuery()) {
            List<Exercise> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load exercises", e);
        }
    }

    @Override
    public Exercise findById(String id) {
        try (Connection c = db.connect();
                PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM exercises WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load exercise " + id, e);
        }
    }

    @Override
    public void insert(Exercise e) {
        String sql = "INSERT INTO exercises (id, name, name_key, equipment_type, laterality, deleted_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getId());
            ps.setString(2, e.getName());
            ps.setString(3, e.getNameKey());
            ps.setString(4, e.type().name());
            ps.setString(5, e.getLaterality().name());
            ps.setString(6, Database.format(e.getDeletedAt()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot insert exercise " + e.getId(), ex);
        }
    }

    @Override
    public void update(Exercise e) {
        String sql = "UPDATE exercises SET name = ?, name_key = ?, equipment_type = ?, laterality = ?, deleted_at = ? WHERE id = ?";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getName());
            ps.setString(2, e.getNameKey());
            ps.setString(3, e.type().name());
            ps.setString(4, e.getLaterality().name());
            ps.setString(5, Database.format(e.getDeletedAt()));
            ps.setString(6, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot update exercise " + e.getId(), ex);
        }
    }

    @Override
    public int countSets(String exerciseId) {
        return count("SELECT COUNT(*) FROM workout_sets WHERE exercise_id = ?", exerciseId);
    }

    @Override
    public int countSessions(String exerciseId) {
        return count("SELECT COUNT(DISTINCT session_id) FROM workout_sets WHERE exercise_id = ?", exerciseId);
    }

    @Override
    public void deletePermanently(String id) {
        // one transaction, explicit deletes in dependency order (no FK cascade from exercises, PLAN §5)
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try {
                List<String> touched = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT DISTINCT session_id FROM workout_sets WHERE exercise_id = ?")) {
                    ps.setString(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            touched.add(rs.getString(1));
                        }
                    }
                }
                execute(c, "DELETE FROM workout_sets WHERE exercise_id = ?", id);
                for (String sessionId : touched) { // closed sessions left without sets go too (G6); open ones stay
                    execute(c, "DELETE FROM workout_sessions WHERE id = ? AND is_open = 0 "
                            + "AND NOT EXISTS (SELECT 1 FROM workout_sets WHERE session_id = workout_sessions.id)", sessionId);
                }
                execute(c, "DELETE FROM template_exercises WHERE exercise_id = ?", id);
                execute(c, "DELETE FROM exercises WHERE id = ?", id);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot permanently delete exercise " + id, e);
        }
    }

    private static void execute(Connection c, String sql, String param) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, param);
            ps.executeUpdate();
        }
    }

    private int count(String sql, String exerciseId) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, exerciseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot count usage of " + exerciseId, e);
        }
    }

    private static Exercise map(ResultSet rs) throws SQLException {
        return Exercise.of(
                EquipmentType.valueOf(rs.getString("equipment_type")),
                rs.getString("id"),
                rs.getString("name"),
                Laterality.valueOf(rs.getString("laterality")),
                Database.instant(rs.getString("deleted_at")));
    }
}
