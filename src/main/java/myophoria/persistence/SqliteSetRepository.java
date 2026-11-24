package myophoria.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import myophoria.core.model.SetType;
import myophoria.core.model.Unit;
import myophoria.core.model.WorkoutSet;
import myophoria.core.port.SetRepository;

/** SQLite implementation of the set port. */
public class SqliteSetRepository implements SetRepository {
    /** Column list shared with SqliteSessionRepository, which loads sets in bulk. */
    static final String SELECT = "SELECT id, session_id, exercise_id, position, set_type, weight, unit, reps, "
            + "bodyweight, completed_at FROM workout_sets";
    private final Database db;

    /** Creates the repository on an opened database. */
    public SqliteSetRepository(Database db) {
        this.db = db;
    }

    @Override
    public WorkoutSet findById(String id) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load set " + id, e);
        }
    }

    @Override
    public void insert(WorkoutSet s) {
        write("INSERT INTO workout_sets (session_id, exercise_id, position, set_type, weight, unit, reps, bodyweight, "
                + "completed_at, id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", s);
    }

    @Override
    public void update(WorkoutSet s) {
        write("UPDATE workout_sets SET session_id = ?, exercise_id = ?, position = ?, set_type = ?, weight = ?, "
                + "unit = ?, reps = ?, bodyweight = ?, completed_at = ? WHERE id = ?", s);
    }

    @Override
    public void delete(String id) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM workout_sets WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete set " + id, e);
        }
    }

    // insert and update bind the same ten values in the same order
    private void write(String sql, WorkoutSet s) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getSessionId());
            ps.setString(2, s.getExerciseId());
            ps.setInt(3, s.getPosition());
            ps.setString(4, s.getSetType().name());
            ps.setDouble(5, s.getWeight());
            ps.setString(6, s.getUnit().name());
            ps.setInt(7, s.getReps());
            if (s.getBodyweight() == null) {
                ps.setNull(8, Types.REAL);
            } else {
                ps.setDouble(8, s.getBodyweight());
            }
            ps.setString(9, Database.format(s.getCompletedAt()));
            ps.setString(10, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save set " + s.getId(), e);
        }
    }

    /** Maps one row of SELECT to a set. */
    static WorkoutSet map(ResultSet rs) throws SQLException {
        double bw = rs.getDouble("bodyweight");
        Double bodyweight = rs.wasNull() ? null : bw;
        return new WorkoutSet(rs.getString("id"), rs.getString("session_id"), rs.getString("exercise_id"),
                rs.getInt("position"), SetType.valueOf(rs.getString("set_type")), rs.getDouble("weight"),
                Unit.valueOf(rs.getString("unit")), rs.getInt("reps"), bodyweight,
                Database.instant(rs.getString("completed_at")));
    }
}
