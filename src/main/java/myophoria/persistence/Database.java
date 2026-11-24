package myophoria.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Owns the SQLite file: creates and migrates it once, then hands out connections. */
public class Database {
    private final String url;
    private final Path dataDir;

    /** Creates a handle for the database file myophoria.db inside the data directory. */
    public Database(Path dataDir) {
        this.dataDir = dataDir;
        this.url = "jdbc:sqlite:" + dataDir.resolve("myophoria.db");
    }

    /** Creates the data directory, enables WAL, migrates the schema and returns startup warnings. */
    public List<String> open() {
        List<String> warnings = new ArrayList<>();
        try {
            Files.createDirectories(dataDir);
            try (Connection c = connect()) {
                try (Statement st = c.createStatement()) { // closed before migrating: an open statement blocks commit
                    st.execute("PRAGMA journal_mode = WAL");
                }
                Migrator.migrate(c);
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Cannot open database", e);
        }
        return warnings;
    }

    /** Opens a new connection with foreign keys on and a 5 second busy timeout. */
    public Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            c.close();
            throw e;
        }
        return c;
    }
}
