package myophoria.core.service;

import java.util.List;

/**
 * Import result for one CSV file. Messages look like "sets.csv:12 reason" (line = record number, header = 1).
 * adjusted lists rows imported with is_default/is_open cleared (G10).
 */
public record FileReport(int imported, int alreadyPresent, List<String> skipped, List<String> orphans,
        List<String> nameConflicts, List<String> adjusted) {
}
