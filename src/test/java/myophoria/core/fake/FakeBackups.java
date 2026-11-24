package myophoria.core.fake;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import myophoria.core.port.Backups;

/** Records backups instead of writing files; fail = true simulates a full disk. */
public class FakeBackups implements Backups {
    public final List<String> taken = new ArrayList<>();
    public boolean fail;

    @Override
    public String createBackup() throws IOException {
        if (fail) {
            throw new IOException("disk full");
        }
        String name = "backup-" + (taken.size() + 1) + ".db";
        taken.add(name);
        return name;
    }
}
