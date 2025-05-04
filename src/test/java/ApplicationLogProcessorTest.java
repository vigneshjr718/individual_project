import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ApplicationLogProcessorTest {

    private ApplicationLogProcessor processor;

    @BeforeEach
    public void setUp() {
        processor = new ApplicationLogProcessor();
    }

    @Test
    public void testEmptyResults() {
        Map<String, Object> results = processor.getResults();
        assertTrue(results.isEmpty());
    }

    @Test
    public void testProcessSingleLog() {
        // Process a single application log
        processor.processLog("timestamp=2024-02-24T16:22:20Z level=INFO message=\"Scheduled maintenance starting\" host=webserver1");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check results
        assertEquals(1, results.size());
        assertTrue(results.containsKey("INFO"));
        assertEquals(1, results.get("INFO"));
    }

    @Test
    public void testProcessMultipleLogs() {
        // Process multiple application logs
        processor.processLog("timestamp=2024-02-24T16:22:20Z level=INFO message=\"Scheduled maintenance starting\" host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:22:35Z level=ERROR message=\"Update process failed\" error_code=5012 host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:22:50Z level=DEBUG message=\"Retrying update process\" attempt=1 host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:23:05Z level=INFO message=\"Update process completed successfully\" host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:23:20Z level=WARNING message=\"High memory usage detected\" host=webserver1");
        processor.processLog("timestamp=2024-02-24T16:23:35Z level=ERROR message=\"Database connection timeout\" host=webserver2");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check results
        assertEquals(4, results.size());
        assertTrue(results.containsKey("INFO"));
        assertTrue(results.containsKey("ERROR"));
        assertTrue(results.containsKey("DEBUG"));
        assertTrue(results.containsKey("WARNING"));

        assertEquals(2, results.get("INFO"));
        assertEquals(2, results.get("ERROR"));
        assertEquals(1, results.get("DEBUG"));
        assertEquals(1, results.get("WARNING"));
    }

    @Test
    public void testSkipInvalidLogs() {
        // Process valid and invalid logs
        processor.processLog("timestamp=2024-02-24T16:22:20Z level=INFO message=\"Scheduled maintenance starting\" host=webserver1");
        processor.processLog("This is not a valid log entry");
        processor.processLog("timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=72");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check that only valid logs were processed
        assertEquals(1, results.size());
        assertTrue(results.containsKey("INFO"));
        assertEquals(1, results.get("INFO"));
    }
}