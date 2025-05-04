import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Processor for Application logs
 */
public class ApplicationLogProcessor implements LogProcessor {
    private final Map<String, Integer> logCounts = new HashMap<>();
    private final Gson gson = new Gson();

    // Pattern to match Application logs in both formats
    private static final Pattern APP_PATTERN = Pattern.compile("\\[APP\\]\\s*(.*)|.*level=([A-Z]+).*message=\"(.+?)\".*");

    @Override
    public void processLog(String logEntry) {
        // Check if this is an Application log entry
        Matcher matcher = APP_PATTERN.matcher(logEntry);
        if (matcher.matches()) {
            try {
                String level;

                // Check if it's a JSON format log
                if (logEntry.startsWith("[APP]")) {
                    String jsonContent = matcher.group(1);
                    JsonObject json = gson.fromJson(jsonContent, JsonObject.class);
                    level = json.get("level").getAsString();
                } else {
                    // It's a key-value format log
                    level = matcher.group(2);
                }

                // Increment count for this level
                logCounts.put(level, logCounts.getOrDefault(level, 0) + 1);
            } catch (Exception e) {
                // Skip invalid entries
            }
        }
    }

    @Override
    public Map<String, Object> getResults() {
        return new HashMap<>(logCounts);
    }
}