import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Processor for Application Performance Metrics (APM) logs
 */
public class APMLogProcessor implements LogProcessor {
    // Store metrics in a map: metric name -> list of values
    private final Map<String, List<Double>> metrics = new HashMap<>();
    private final Gson gson = new Gson();

    // Pattern to match APM logs in both formats
    private static final Pattern APM_PATTERN = Pattern.compile("\\[APM\\]\\s*(.*)|.*metric=([\\w_]+).*value=([\\d.]+).*");

    @Override
    public void processLog(String logEntry) {
        // Check if this is an APM log entry
        Matcher matcher = APM_PATTERN.matcher(logEntry);
        if (matcher.matches()) {
            try {
                String metricName;
                double value;

                // Check if it's a JSON format log
                if (logEntry.startsWith("[APM]")) {
                    String jsonContent = matcher.group(1);
                    JsonObject json = gson.fromJson(jsonContent, JsonObject.class);
                    metricName = json.get("metric").getAsString();
                    value = json.get("value").getAsDouble();
                } else {
                    // It's a key-value format log
                    metricName = matcher.group(2);
                    value = Double.parseDouble(matcher.group(3));
                }

                // Add to metrics map
                metrics.computeIfAbsent(metricName, k -> new ArrayList<>()).add(value);
            } catch (Exception e) {
                // Skip invalid entries
            }
        }
    }

    @Override
    public Map<String, Object> getResults() {
        Map<String, Object> results = new HashMap<>();

        // Calculate statistics for each metric
        for (Map.Entry<String, List<Double>> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            List<Double> values = entry.getValue();

            if (!values.isEmpty()) {
                Map<String, Object> stats = calculateStats(values);
                results.put(metricName, stats);
            }
        }

        return results;
    }

    /**
     * Calculates statistics for a list of values
     */
    private Map<String, Object> calculateStats(List<Double> values) {
        Map<String, Object> stats = new HashMap<>();

        // Sort values for median calculation
        Collections.sort(values);

        // Calculate min
        stats.put("minimum", values.get(0));

        // Calculate median
        double median;
        int size = values.size();
        if (size % 2 == 0) {
            median = (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            median = values.get(size / 2);
        }
        stats.put("median", median);

        // Calculate average
        double sum = 0;
        for (Double value : values) {
            sum += value;
        }
        double average = sum / values.size();
        stats.put("average", average);

        // Calculate max
        stats.put("max", values.get(values.size() - 1));

        return stats;
    }
}