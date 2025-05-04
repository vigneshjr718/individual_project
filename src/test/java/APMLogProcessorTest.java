import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class APMLogProcessorTest {

    private APMLogProcessor processor;

    @BeforeEach
    public void setUp() {
        processor = new APMLogProcessor();
    }

    @Test
    public void testEmptyResults() {
        Map<String, Object> results = processor.getResults();
        assertTrue(results.isEmpty());
    }

    @Test
    public void testProcessSingleMetric() {
        // Process a single APM log
        processor.processLog("timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=72");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check results
        assertEquals(1, results.size());
        assertTrue(results.containsKey("cpu_usage_percent"));

        @SuppressWarnings("unchecked")
        Map<String, Object> cpuStats = (Map<String, Object>) results.get("cpu_usage_percent");

        assertEquals(72.0, cpuStats.get("minimum"));
        assertEquals(72.0, cpuStats.get("median"));
        assertEquals(72.0, cpuStats.get("average"));
        assertEquals(72.0, cpuStats.get("max"));
    }

    @Test
    public void testProcessMultipleMetrics() {
        // Process multiple APM logs
        processor.processLog("timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=60");
        processor.processLog("timestamp=2024-02-24T16:22:30Z metric=memory_usage_percent host=webserver1 value=85");
        processor.processLog("timestamp=2024-02-24T16:22:45Z metric=cpu_usage_percent host=webserver1 value=90");
        processor.processLog("timestamp=2024-02-24T16:23:45Z metric=memory_usage_percent host=webserver2 value=5");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check results
        assertEquals(2, results.size());
        assertTrue(results.containsKey("cpu_usage_percent"));
        assertTrue(results.containsKey("memory_usage_percent"));

        @SuppressWarnings("unchecked")
        Map<String, Object> cpuStats = (Map<String, Object>) results.get("cpu_usage_percent");

        assertEquals(60.0, cpuStats.get("minimum"));
        assertEquals(75.0, cpuStats.get("median"));
        assertEquals(75.0, cpuStats.get("average"));
        assertEquals(90.0, cpuStats.get("max"));

        @SuppressWarnings("unchecked")
        Map<String, Object> memoryStats = (Map<String, Object>) results.get("memory_usage_percent");

        assertEquals(5.0, memoryStats.get("minimum"));
        assertEquals(45.0, memoryStats.get("median"));
        assertEquals(45.0, memoryStats.get("average"));
        assertEquals(85.0, memoryStats.get("max"));
    }

    @Test
    public void testSkipInvalidLogs() {
        // Process valid and invalid logs
        processor.processLog("timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=72");
        processor.processLog("This is not a valid log entry");
        processor.processLog("timestamp=2024-02-24T16:22:15Z level=INFO message=\"Not an APM log\"");

        // Get results
        Map<String, Object> results = processor.getResults();

        // Check that only valid logs were processed
        assertEquals(1, results.size());
        assertTrue(results.containsKey("cpu_usage_percent"));
    }
}