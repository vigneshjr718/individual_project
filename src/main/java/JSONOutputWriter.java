import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Implementation of OutputWriter for JSON files
 */
public class JSONOutputWriter implements OutputWriter {
    private final Gson gson;

    public JSONOutputWriter() {
        // Create a Gson instance with pretty printing
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public void writeToFile(Map<String, Object> data, String outputPath) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath)) {
            // Convert data to JSON and write to file
            gson.toJson(data, writer);
        }
    }
}