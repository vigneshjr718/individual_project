import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Processor for Request logs
 */
public class RequestLogProcessor implements LogProcessor {
    // Store data by API route
    private final Map<String, RouteData> routes = new HashMap<>();
    private final Gson gson = new Gson();

    // Pattern to match Request logs in both formats
    private static final Pattern REQ_PATTERN = Pattern.compile("\\[REQ\\]\\s*(.*)|.*request_method=([A-Z]+)\\s+request_url=\"?(.*?)\"?\\s+response_status=(\\d+)\\s+response_time_ms=(\\d+).*");

    @Override
    public void processLog(String logEntry) {
        // Check if this is a Request log entry
        Matcher matcher = REQ_PATTERN.matcher(logEntry);
        if (matcher.matches()) {
            try {
                String url;
                int statusCode;
                int responseTime;

                // Check if it's a JSON format log
                if (logEntry.startsWith("[REQ]")) {
                    String jsonContent = matcher.group(1);
                    JsonObject json = gson.fromJson(jsonContent, JsonObject.class);
                    url = json.get("path").getAsString();
                    statusCode = json.get("status").getAsInt();
                    responseTime = json.get("duration").getAsInt();
                } else {
                    // It's a key-value format log
                    url = matcher.group(3);
                    statusCode = Integer.parseInt(matcher.group(4));
                    responseTime = Integer.parseInt(matcher.group(5));
                }

                // Create or update route data
                RouteData routeData = routes.computeIfAbsent(url, k -> new RouteData());
                routeData.addResponse(statusCode, responseTime);
            } catch (Exception e) {
                // Skip invalid entries
            }
        }
    }

    @Override
    public Map<String, Object> getResults() {
        Map<String, Object> results = new HashMap<>();

        // Create result structure for each route
        for (Map.Entry<String, RouteData> entry : routes.entrySet()) {
            String route = entry.getKey();
            RouteData data = entry.getValue();

            Map<String, Object> routeResults = new HashMap<>();

            // Add response time stats
            routeResults.put("response_times", data.getResponseTimeStats());

            // Add status code counts
            routeResults.put("status_codes", data.getStatusCodeCounts());

            results.put(route, routeResults);
        }

        return results;
    }

    /**
     * Helper class to store data for a single API route
     */
    private static class RouteData {
        private final List<Integer> responseTimes = new ArrayList<>();
        private final Map<String, Integer> statusCodeCounts = new HashMap<>();

        /**
         * Adds a response to this route's data
         */
        public void addResponse(int statusCode, int responseTime) {
            // Add response time
            responseTimes.add(responseTime);

            // Categorize status code (2XX, 4XX, 5XX)
            String category = getStatusCodeCategory(statusCode);
            statusCodeCounts.put(category, statusCodeCounts.getOrDefault(category, 0) + 1);
        }

        /**
         * Returns the status code category (2XX, 4XX, 5XX)
         */
        private String getStatusCodeCategory(int statusCode) {
            int category = statusCode / 100;
            return category + "XX";
        }

        /**
         * Calculates response time statistics
         */
        public Map<String, Object> getResponseTimeStats() {
            Map<String, Object> stats = new HashMap<>();

            if (responseTimes.isEmpty()) {
                return stats;
            }

            // Sort for percentile calculations
            Collections.sort(responseTimes);
            int size = responseTimes.size();

            // Min
            stats.put("min", responseTimes.get(0));

            // Percentiles
            stats.put("50_percentile", getPercentile(50));
            stats.put("90_percentile", getPercentile(90));
            stats.put("95_percentile", getPercentile(95));
            stats.put("99_percentile", getPercentile(99));

            // Max
            stats.put("max", responseTimes.get(size - 1));

            return stats;
        }

        /**
         * Calculates the specified percentile of response times
         */
        private int getPercentile(int percentile) {
            if (responseTimes.isEmpty()) {
                return 0;
            }

            int index = (int) Math.ceil(percentile / 100.0 * responseTimes.size()) - 1;
            if (index < 0) {
                index = 0;
            }

            return responseTimes.get(Math.min(index, responseTimes.size() - 1));
        }

        /**
         * Returns the status code counts
         */
        public Map<String, Object> getStatusCodeCounts() {
            Map<String, Object> counts = new HashMap<>();

            // Ensure we have entries for all categories
            counts.put("2XX", statusCodeCounts.getOrDefault("2XX", 0));
            counts.put("4XX", statusCodeCounts.getOrDefault("4XX", 0));
            counts.put("5XX", statusCodeCounts.getOrDefault("5XX", 0));

            return counts;
        }
    }
}