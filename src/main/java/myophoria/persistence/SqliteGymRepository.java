package myophoria.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import myophoria.core.model.Gym;
import myophoria.core.port.GymRepository;

/** SQLite implementation of the gym port. */
public class SqliteGymRepository implements GymRepository {
    private static final String SELECT = "SELECT id, name, is_default, deleted_at FROM gyms";
    private final Database db;

    /** Creates the repository on an opened database. */
    public SqliteGymRepository(Database db) {
        this.db = db;
    }

    @Override
    public List<Gym> findAll() {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(SELECT);
                ResultSet rs = ps.executeQuery()) {
            List<Gym> result = new ArrayList<>();
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load gyms", e);
        }
    }

    @Override
    public Gym findById(String id) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot load gym " + id, e);
        }
    }

    @Override
    public void insert(Gym g) {
        write("INSERT INTO gyms (name, name_key, is_default, deleted_at, id) VALUES (?, ?, ?, ?, ?)", g);
    }

    @Override
    public void update(Gym g) {
        write("UPDATE gyms SET name = ?, name_key = ?, is_default = ?, deleted_at = ? WHERE id = ?", g);
    }

    @Override
    public void makeDefault(String id) {
        try (Connection c = db.connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement clear = c.prepareStatement("UPDATE gyms SET is_default = 0 WHERE is_default = 1");
                    PreparedStatement set = c.prepareStatement("UPDATE gyms SET is_default = 1 WHERE id = ?")) {
                clear.executeUpdate();
                set.setString(1, id);
                set.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot make gym " + id + " the default", e);
        }
    }

    private void write(String sql, Gym g) {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, g.getName());
            ps.setString(2, g.getNameKey());
            ps.setInt(3, g.isDefault() ? 1 : 0);
            ps.setString(4, Database.format(g.getDeletedAt()));
            ps.setString(5, g.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot save gym " + g.getId(), e);
        }
    }

    private static Gym map(ResultSet rs) throws SQLException {
        return new Gym(rs.getString("id"), rs.getString("name"), rs.getInt("is_default") == 1,
                Database.instant(rs.getString("deleted_at")));
    }
}
