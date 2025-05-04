import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class LogAnalyzerApp {
    public static void main(String[] args) {
        // Parse command line arguments
        String inputFilePath = parseCommandLineArgs(args);

        if (inputFilePath == null) {
            System.out.println("Error: Please provide a valid input file path using --file option");
            System.exit(1);
        }

        try {
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
            analyzer.analyze(inputFilePath);

            // Write results to files
            analyzer.outputResults("apm.json", "application.json", "request.json");

            System.out.println("Log analysis completed successfully!");
            System.out.println("Output files generated: apm.json, application.json, request.json");

        } catch (IOException e) {
            System.err.println("Error processing log file: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String parseCommandLineArgs(String[] args) {
        if (args.length < 2) {
            return null;
        }

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--file")) {
                return args[i+1];
            }
        }

        return null;
    }
}