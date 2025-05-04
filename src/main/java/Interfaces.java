import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for log file readers
 */
interface LogFileReader {
    /**
     * Reads a log file and returns a list of log entries
     *
     * @param filePath Path to the log file
     * @return List of log entries
     * @throws IOException If an I/O error occurs
     */
    List<String> readLogFile(String filePath) throws IOException;
}

/**
 * Interface for log processors
 */
interface LogProcessor {
    /**
     * Processes a single log entry
     *
     * @param logEntry The log entry to process
     */
    void processLog(String logEntry);

    /**
     * Returns the processed results as a JSON-serializable object
     *
     * @return The processed results
     */
    Map<String, Object> getResults();
}

/**
 * Interface for output writers
 */
interface OutputWriter {
    /**
     * Writes the results to a file
     *
     * @param data The data to write
     * @param outputPath Path to the output file
     * @throws IOException If an I/O error occurs
     */
    void writeToFile(Map<String, Object> data, String outputPath) throws IOException;
}