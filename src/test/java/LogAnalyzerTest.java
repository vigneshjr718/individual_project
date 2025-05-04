import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LogAnalyzerTest {

    @TempDir
    Path tempDir;

    private LogFileReader mockReader;
    private APMLogProcessor apmProcessor;
    private ApplicationLogProcessor appProcessor;
    private RequestLogProcessor reqProcessor;
    private LogAnalyzer analyzer;

    private List<String> sampleLogs = Arrays.asList(
            "timestamp=2024-02-24T16:22:15Z metric=cpu_usage_percent host=webserver1 value=72",
            "timestamp=2024-02-24T16:22:20Z level=INFO message=\"Scheduled maintenance starting\" host=webserver1",
            "timestamp=2024-02-24T16:22:25Z request_method=POST request_url=\"/api/update\" response_status=202 response_time_ms=200 host=webserver1",
            "timestamp=2024-02-24T16:22:30Z metric=memory_usage_percent host=webserver1 value=85",
            "timestamp=2024-02-24T16:22:35Z level=ERROR message=\"Update process failed\" error_code=5012 host=webserver1"
    );

    @BeforeEach
    public void setUp() {
        // Create mock reader
        mockReader = mock(LogFileReader.class);

        // Create processors
        apmProcessor = new APMLogProcessor();
        appProcessor = new ApplicationLogProcessor();
        reqProcessor = new RequestLogProcessor();

        // Create analyzer with mock reader and real processors
        analyzer = new LogAnalyzer(mockReader, Arrays.asList(apmProcessor, appProcessor, reqProcessor));
    }

    @Test
    public void testAnalyze() throws IOException {
        // Set up mock reader
        when(mockReader.readLogFile(anyString())).thenReturn(sampleLogs);

        // Call analyze
        analyzer.analyze("mock_input.txt");

        // Verify that the reader was called
        verify(mockReader).readLogFile("mock_input.txt");

        // Verify that logs were correctly processed

        // Check APM results
        assertFalse(apmProcessor.getResults().isEmpty());
        assertTrue(apmProcessor.getResults().containsKey("cpu_usage_percent"));
        assertTrue(apmProcessor.getResults().containsKey("memory_usage_percent"));

        // Check Application log results
        assertFalse(appProcessor.getResults().isEmpty());
        assertTrue(appProcessor.getResults().containsKey("INFO"));
        assertTrue(appProcessor.getResults().containsKey("ERROR"));

        // Check Request log results
        assertFalse(reqProcessor.getResults().isEmpty());
        assertTrue(reqProcessor.getResults().containsKey("/api/update"));
    }

    @Test
    public void testOutputResults() throws IOException {
        // Set up mock reader
        when(mockReader.readLogFile(anyString())).thenReturn(sampleLogs);

        // Call analyze
        analyzer.analyze("mock_input.txt");

        // Set up output paths
        Path apmPath = tempDir.resolve("apm.json");
        Path appPath = tempDir.resolve("application.json");
        Path reqPath = tempDir.resolve("request.json");

        // Call outputResults
        analyzer.outputResults(apmPath.toString(), appPath.toString(), reqPath.toString());

        // Verify that output files were created
        assertTrue(Files.exists(apmPath));
        assertTrue(Files.exists(appPath));
        assertTrue(Files.exists(reqPath));

        // Verify that output files have content
        assertTrue(Files.size(apmPath) > 0);
        assertTrue(Files.size(appPath) > 0);
        assertTrue(Files.size(reqPath) > 0);
    }
}