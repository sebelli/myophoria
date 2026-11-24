package myophoria.core.port;

import java.io.IOException;

/** Takes a consistent copy of the whole database. */
public interface Backups {
    /** Writes a new backup and returns its file name. */
    String createBackup() throws IOException;
}
