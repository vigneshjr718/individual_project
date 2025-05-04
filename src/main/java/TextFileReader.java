import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of LogFileReader for text files
 */
public class TextFileReader implements LogFileReader {

    @Override
    public List<String> readLogFile(String filePath) throws IOException {
        // Read all lines from the file
        List<String> lines = Files.readAllLines(Paths.get(filePath));

        // Filter out empty lines
        return lines.stream()
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.toList());
    }
}