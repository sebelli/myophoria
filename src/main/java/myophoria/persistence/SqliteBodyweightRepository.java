package myophoria.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import myophoria.core.model.BodyweightEntry;
import myophoria.core.model.Unit;
import myophoria.core.port.BodyweightRepository;

/** SQLite implementation of the bodyweight port. */
public class SqliteBodyweightRepository implements BodyweightRepository {
    private static final String SELECT = "SELECT id, weight, unit, recorded_at, local_date FROM bodyweight_entries";
    private final Database db;

    /** Creates the repository on an opened database. */
    public SqliteBodyweightRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<BodyweightEntry> findAll() {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(SELECT);
                ResultSet rs = ps.executeQuery()) {
            List<BodyweightEntry> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load bodyweight entries", e);
        }
    }

    @Override
    public BodyweightEntry findById(String id) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load bodyweight entry " + id, e);
        }
    }

    @Override
    public void insert(BodyweightEntry e) {
        write("INSERT INTO bodyweight_entries (weight, unit, recorded_at, local_date, id) VALUES (?, ?, ?, ?, ?)", e);
    }

    @Override
    public void update(BodyweightEntry e) {
        write("UPDATE bodyweight_entries SET weight = ?, unit = ?, recorded_at = ?, local_date = ? WHERE id = ?", e);
    }

    @Override
    public void delete(String id) {
        try (Connection c = db.connect();
                PreparedStatement ps = c.prepareStatement("DELETE FROM bodyweight_entries WHERE id = ?")) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot delete bodyweight entry " + id, e);
        }
    }

    // insert and update bind the same five values in the same order
    private void write(String sql, BodyweightEntry e) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, e.getWeight());
            ps.setString(2, e.getUnit().name());
            ps.setString(3, Database.format(e.getRecordedAt()));
            ps.setString(4, e.getLocalDate().toString());
            ps.setString(5, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Cannot save bodyweight entry " + e.getId(), ex);
        }
    }

    private static BodyweightEntry map(ResultSet rs) throws SQLException {
        return new BodyweightEntry(rs.getString("id"), rs.getDouble("weight"), Unit.valueOf(rs.getString("unit")),
                Database.instant(rs.getString("recorded_at")), LocalDate.parse(rs.getString("local_date")));
    }
}
