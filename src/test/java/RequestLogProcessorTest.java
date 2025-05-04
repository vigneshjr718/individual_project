import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class RequestLogProcessorTest {

    private RequestLogProcessor processor;

    @BeforeEach
    public void setUp() {
        processor = new RequestLogProcessor();
    }

    @Test
    public void testEmptyResults() {
        Map<String, Object> results = processor.getResults();
        assertTrue(results.isEmpty());
    }

    @Test
    public void testProcessSingleLog() {
        // Process a single request log
        processor.processLog("timestamp=2024-02-24T16:22:25Z request_method=POST request_url=\"/api/update\" response_status=202 response_time_ms=200 host=webserver1");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check results
        assertEquals(1, results.size());
        assertTrue(results.containsKey("/api/update"));

        @SuppressWarnings("unchecked")
        Map<String, Object> routeData = (Map<String, Object>) results.get("/api/update");

        assertTrue(routeData.containsKey("response_times"));
        assertTrue(routeData.containsKey("status_codes"));

        @SuppressWarnings("unchecked")
        Map<String, Object> responseTimes = (Map<String, Object>) routeData.get("response_times");

        assertEquals(200, responseTimes.get("min"));
        assertEquals(200, responseTimes.get("50_percentile"));
        assertEquals(200, responseTimes.get("90_percentile"));
        assertEquals(200, responseTimes.get("95_percentile"));
        assertEquals(200, responseTimes.get("99_percentile"));
        assertEquals(200, responseTimes.get("max"));

        @SuppressWarnings("unchecked")
        Map<String, Object> statusCodes = (Map<String, Object>) routeData.get("status_codes");

        assertEquals(1, statusCodes.get("2XX"));
        assertEquals(0, statusCodes.get("4XX"));
        assertEquals(0, statusCodes.get("5XX"));
    }

    @Test
    public void testProcessMultipleLogs() {
        // Process multiple request logs
        processor.processLog("timestamp=2024-02-24T16:22:25Z request_method=POST request_url=\"/api/update\" response_status=202 response_time_ms=200 host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:22:40Z request_method=GET request_url=\"/api/status\" response_status=200 response_time_ms=100 host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:22:55Z request_method=POST request_url=\"/api/retry\" response_status=503 response_time_ms=250 host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:23:10Z request_method=GET request_url=\"/home\" response_status=404 response_time_ms=25 host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:23:25Z request_method=GET request_url=\"/api/status\" response_status=200 response_time_ms=150 host=webserver1");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check results
        assertEquals(4, results.size());
        assertTrue(results.containsKey("/api/update"));
        assertTrue(results.containsKey("/api/status"));
        assertTrue(results.containsKey("/api/retry"));
        assertTrue(results.containsKey("/home"));

        // Check /api/status route specifically
        @SuppressWarnings("unchecked")
        Map<String, Object> statusRouteData = (Map<String, Object>) results.get("/api/status");

        @SuppressWarnings("unchecked")
        Map<String, Object> responseTimes = (Map<String, Object>) statusRouteData.get("response_times");

        assertEquals(100, responseTimes.get("min"));
        assertEquals(100, responseTimes.get("50_percentile"));
        assertEquals(150, responseTimes.get("90_percentile"));
        assertEquals(150, responseTimes.get("95_percentile"));
        assertEquals(150, responseTimes.get("99_percentile"));
        assertEquals(150, responseTimes.get("max"));

        @SuppressWarnings("unchecked")
        Map<String, Object> statusCodes = (Map<String, Object>) statusRouteData.get("status_codes");

        assertEquals(2, statusCodes.get("2XX"));
        assertEquals(0, statusCodes.get("4XX"));
        assertEquals(0, statusCodes.get("5XX"));

        // Check /home route (should have 4XX status)
        @SuppressWarnings("unchecked")
        Map<String, Object> homeRouteData = (Map<String, Object>) results.get("/home");

        @SuppressWarnings("unchecked")
        Map<String, Object> homeStatusCodes = (Map<String, Object>) homeRouteData.get("status_codes");

        assertEquals(0, homeStatusCodes.get("2XX"));
        assertEquals(1, homeStatusCodes.get("4XX"));
        assertEquals(0, homeStatusCodes.get("5XX"));
    }

    @Test
    public void testSkipInvalidLogs() {
        // Process valid and invalid logs
        processor.processLog("timestamp=2024-02-24T16:22:25Z request_method=POST request_url=\"/api/update\" response_status=202 response_time_ms=200 host=webserver1");
        processor.processLog("This is not a valid log entry");
        processor.processLog("timestamp=2024-02-24T16:22:20Z level=INFO message=\"Not a request log\" host=webserver1");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check that only valid logs were processed
        assertEquals(1, results.size());
        assertTrue(results.containsKey("/api/update"));
    }
}