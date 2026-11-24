package myophoria.core.service;

import java.util.Map;

/** Import result per CSV file name, in import order. */
public record ImportReport(Map<String, FileReport> files) {
}
