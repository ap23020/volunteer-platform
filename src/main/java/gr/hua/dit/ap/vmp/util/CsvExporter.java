package gr.hua.dit.ap.vmp.util;

import java.util.List;

public class CsvExporter {

    // Δημιουργεί CSV string από headers και rows
    public static String toCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();

        // Headers
        sb.append(String.join(",", headers)).append("\n");

        // Rows
        for (List<String> row : rows) {
            sb.append(row.stream()
                            .map(CsvExporter::escape)
                            .reduce((a, b) -> a + "," + b)
                            .orElse(""))
                    .append("\n");
        }

        return sb.toString();
    }

    // Escape για ειδικούς χαρακτήρες
    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
