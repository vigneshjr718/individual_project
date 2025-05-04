import java.io.IOException;
import java.util.List;

/**
 * Main class responsible for orchestrating the log analysis process
 */
public class LogAnalyzer {
    private final LogFileReader logReader;
    private final List<LogProcessor> logProcessors;

    public LogAnalyzer(LogFileReader logReader, List<LogProcessor> logProcessors) {
        this.logReader = logReader;
        this.logProcessors = logProcessors;
    }

    /**
     * Reads log entries from the input file and processes them with all registered processors
     *
     * @param inputFilePath Path to the log file
     * @throws IOException If an I/O error occurs
     */
    public void analyze(String inputFilePath) throws IOException {
        List<String> logEntries = logReader.readLogFile(inputFilePath);

        // Process each log entry with all processors
        for (String logEntry : logEntries) {
            for (LogProcessor processor : logProcessors) {
                processor.processLog(logEntry);
            }
        }
    }

    /**
     * Writes the results of each processor to its respective output file
     *
     * @param apmOutputPath Path to APM output file
     * @param appOutputPath Path to Application log output file
     * @param reqOutputPath Path to Request log output file
     * @throws IOException If an I/O error occurs
     */
    public void outputResults(String apmOutputPath, String appOutputPath, String reqOutputPath) throws IOException {
        OutputWriter writer = new JSONOutputWriter();

        for (LogProcessor processor : logProcessors) {
            String outputPath = null;

            if (processor instanceof APMLogProcessor) {
                outputPath = apmOutputPath;
            } else if (processor instanceof ApplicationLogProcessor) {
                outputPath = appOutputPath;
            } else if (processor instanceof RequestLogProcessor) {
                outputPath = reqOutputPath;
            }

            if (outputPath != null) {
                writer.writeToFile(processor.getResults(), outputPath);
            }
        }
    }
}