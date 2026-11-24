package myophoria.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Applies numbered SQL scripts from db/migration, tracking progress in PRAGMA user_version. */
public final class Migrator {
    // shortcut: scripts are listed by hand (jar resources cannot be listed), position = version; list the next script here
    private static final List<String> SCRIPTS = List.of("V1__init.sql");

    private Migrator() {
    }

    /** Runs every script newer than the database's user_version, each in its own transaction. */
    public static void migrate(Connection connection) throws SQLException {
        int current = userVersion(connection);
        boolean autoCommit = connection.getAutoCommit();
        try {
            for (int version = current + 1; version <= SCRIPTS.size(); version++) {
                connection.setAutoCommit(false);
                try (Statement st = connection.createStatement()) {
                    for (String sql : statements(read(SCRIPTS.get(version - 1)))) {
                        st.execute(sql);
                    }
                    st.execute("PRAGMA user_version = " + version);
                    connection.commit();
                } catch (SQLException | RuntimeException e) {
                    connection.rollback();
                    throw e;
                }
            }
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static int userVersion(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.getInt(1);
        }
    }

    private static String read(String script) {
        try (InputStream in = Migrator.class.getResourceAsStream("/db/migration/" + script)) {
            if (in == null) {
                throw new IllegalStateException("Missing migration " + script);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read migration " + script, e);
        }
    }

    // shortcut: splits on ';' after dropping "--" comment lines, so scripts must not contain ';' inside strings or comments
    private static List<String> statements(String script) {
        StringBuilder sb = new StringBuilder();
        for (String line : script.split("\n")) {
            if (!line.trim().startsWith("--")) {
                sb.append(line).append('\n');
            }
        }
        return java.util.Arrays.stream(sb.toString().split(";")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
