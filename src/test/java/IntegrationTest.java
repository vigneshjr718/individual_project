import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    @TempDir
    Path tempDir;

    private static final String SAMPLE_LOG_CONTENT = String.join("\n",
            "timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=72",
            "timestamp=2024-02-24T16:22:20Z level=INFO message=\"Scheduled maintenance starting\" host=webserver1",
            "timestamp=2024-02-24T16:22:25Z request_method=POST request_url=\"/api/update\" response_status=202 response_time_ms=200 host=webserver1",
            "timestamp=2024-02-24T16:22:30Z metric=memory_usage_percent host=webserver1 value=85",
            "timestamp=2024-02-24T16:22:35Z level=ERROR message=\"Update process failed\" error_code=5012 host=webserver1",
            "timestamp=2024-02-24T16:22:40Z request_method=GET request_url=\"/api/status\" response_status=200 response_time_ms=100 host=webserver1",
            "timestamp=2024-02-24T16:22:45Z metric=disk_usage_percent mountpoint=/ host=webserver1 value=68",
            "timestamp=2024-02-24T16:22:50Z level=DEBUG message=\"Retrying update process\" attempt=1 host=webserver1"
    );

    @Test
    public void testEndToEndProcess() throws IOException {
        // Create input file
        Path inputPath = tempDir.resolve("input.txt");
        Files.writeString(inputPath, SAMPLE_LOG_CONTENT);

        // Define output paths
        Path apmPath = tempDir.resolve("apm.json");
        Path appPath = tempDir.resolve("application.json");
        Path reqPath = tempDir.resolve("request.json");

        // Create log reader
        LogFileReader reader = new TextFileReader();

        // Create log processors
        List<LogProcessor> processors = Arrays.asList(
                new APMLogProcessor(),
                new ApplicationLogProcessor(),
                new RequestLogProcessor()
        );

        // Create log analyzer
        LogAnalyzer analyzer = new LogAnalyzer(reader, processors);

        // Analyze logs
        analyzer.analyze(inputPath.toString());

        // Output results
        analyzer.outputResults(apmPath.toString(), appPath.toString(), reqPath.toString());

        // Verify that output files were created
        assertTrue(Files.exists(apmPath));
        assertTrue(Files.exists(appPath));
        assertTrue(Files.exists(reqPath));

        // Parse and verify the content of APM output
        String apmContent = Files.readString(apmPath);
        JsonObject apmJson = new Gson().fromJson(apmContent, JsonObject.class);

        assertTrue(apmJson.has("cpu_usage_percent"));
        assertTrue(apmJson.has("memory_usage_percent"));
        assertTrue(apmJson.has("disk_usage_percent"));

        JsonObject cpuMetrics = apmJson.getAsJsonObject("cpu_usage_percent");
        assertEquals(72.0, cpuMetrics.get("minimum").getAsDouble());
        assertEquals(72.0, cpuMetrics.get("median").getAsDouble());
        assertEquals(72.0, cpuMetrics.get("average").getAsDouble());
        assertEquals(72.0, cpuMetrics.get("max").getAsDouble());

        // Parse and verify the content of Application log output
        String appContent = Files.readString(appPath);
        JsonObject appJson = new Gson().fromJson(appContent, JsonObject.class);

        assertEquals(1, appJson.get("INFO").getAsInt());
        assertEquals(1, appJson.get("ERROR").getAsInt());
        assertEquals(1, appJson.get("DEBUG").getAsInt());

        // Parse and verify the content of Request log output
        String reqContent = Files.readString(reqPath);
        JsonObject reqJson = new Gson().fromJson(reqContent, JsonObject.class);

        assertTrue(reqJson.has("/api/update"));
        assertTrue(reqJson.has("/api/status"));

        JsonObject apiUpdateRoute = reqJson.getAsJsonObject("/api/update");
        JsonObject apiUpdateResponseTimes = apiUpdateRoute.getAsJsonObject("response_times");
        JsonObject apiUpdateStatusCodes = apiUpdateRoute.getAsJsonObject("status_codes");

        assertEquals(200, apiUpdateResponseTimes.get("min").getAsInt());
        assertEquals(200, apiUpdateResponseTimes.get("max").getAsInt());
        assertEquals(1, apiUpdateStatusCodes.get("2XX").getAsInt());
    }
}