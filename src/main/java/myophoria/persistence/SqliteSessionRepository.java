package myophoria.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import myophoria.core.model.WorkoutSession;
import myophoria.core.model.WorkoutSet;
import myophoria.core.port.SessionRepository;

/** SQLite implementation of the session port; sets are loaded in one query and grouped with a HashMap. */
public class SqliteSessionRepository implements SessionRepository {
    private static final String SELECT = "SELECT id, gym_id, local_date, started_at, ended_at, is_open FROM workout_sessions";
    private static final String SET_ORDER = " ORDER BY completed_at, position";
    private final Database db;

    /** Creates the repository on an opened database. */
    public SqliteSessionRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<WorkoutSession> findAll() {
        return load("", null);
    }

    @Override
    public WorkoutSession findById(String id) {
        List<WorkoutSession> found = load(" WHERE id = ?", id);
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public WorkoutSession findOpen() {
        List<WorkoutSession> found = load(" WHERE is_open = 1", null);
        return found.isEmpty() ? null : found.get(0);
    }

    @Override
    public void insert(WorkoutSession s) {
        write("INSERT INTO workout_sessions (gym_id, local_date, started_at, ended_at, is_open, id) VALUES (?, ?, ?, ?, ?, ?)", s);
    }

    @Override
    public void update(WorkoutSession s) {
        write("UPDATE workout_sessions SET gym_id = ?, local_date = ?, started_at = ?, ended_at = ?, is_open = ? WHERE id = ?", s);
    }

    @Override
    public void delete(String id) {
        // workout_sets.session_id has ON DELETE CASCADE, so the sets go with the session
        try (Connection c = db.connect();
                PreparedStatement ps = c.prepareStatement("DELETE FROM workout_sessions WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete session " + id, e);
        }
    }

    // where is " WHERE ..." for sessions; the sets query uses the same filter on session_id
    private List<WorkoutSession> load(String where, String param) {
        String setWhere = where.isEmpty() ? ""
                : " WHERE session_id IN (SELECT id FROM workout_sessions" + where + ")";
        try (Connection c = db.connect()) {
            Map<String, WorkoutSession> byId = new HashMap<>();
            List<WorkoutSession> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(SELECT + where)) {
                if (param != null) {
                    ps.setString(1, param);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        WorkoutSession s = map(rs);
                        byId.put(s.getId(), s);
                        result.add(s);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(SqliteSetRepository.SELECT + setWhere + SET_ORDER)) {
                if (param != null) {
                    ps.setString(1, param);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        WorkoutSet set = SqliteSetRepository.map(rs);
                        byId.get(set.getSessionId()).addSet(set);
                    }
                }
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load sessions", e);
        }
    }

    private void write(String sql, WorkoutSession s) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.getGymId());
            ps.setString(2, s.getLocalDate().toString());
            ps.setString(3, Database.format(s.getStartedAt()));
            ps.setString(4, Database.format(s.getEndedAt()));
            ps.setInt(5, s.isOpen() ? 1 : 0);
            ps.setString(6, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save session " + s.getId(), e);
        }
    }

    private static WorkoutSession map(ResultSet rs) throws SQLException {
        return new WorkoutSession(rs.getString("id"), rs.getString("gym_id"), LocalDate.parse(rs.getString("local_date")),
                Database.instant(rs.getString("started_at")), Database.instant(rs.getString("ended_at")),
                rs.getInt("is_open") == 1);
    }
}
