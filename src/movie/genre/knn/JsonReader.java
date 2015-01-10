package movie.genre.knn;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import org.json.JSONException;

import org.json.JSONObject;

public class JsonReader {

    public JSONObject read(final String filePath) throws FileNotFoundException, JSONException {
        final Scanner scanner = new Scanner(new File(filePath));
        final StringBuilder resultBuilder = new StringBuilder();
        while (scanner.hasNextLine()) {
            resultBuilder.append(scanner.nextLine());
        }

        return new JSONObject(resultBuilder.toString());
    }
}
