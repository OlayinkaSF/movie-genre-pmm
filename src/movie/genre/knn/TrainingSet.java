package movie.genre.knn;

import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import movie.genre.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TrainingSet implements Iterable<Movie> {

    private static final String KEY_MOVIES = "movies";
    private static final String FILE_PATH_TRAINING_DATA = "resources/training_set.txt";

    private final List<Movie> movies;

    public TrainingSet() throws JSONException {
        movies = new LinkedList<>();

        final JsonReader reader = new JsonReader();
        JSONObject trainingData = new JSONObject();
        try {
            //read training_set text
            trainingData = reader.read(FILE_PATH_TRAINING_DATA);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            trainingData.put(KEY_MOVIES, new JSONArray());
        }

        final JSONArray movieMeta = trainingData.getJSONArray(KEY_MOVIES);
        for (int i = 0; i < movieMeta.length(); ++i) {
            final JSONObject currentMovie = movieMeta.getJSONObject(i);
            final Movie movie = new Movie(currentMovie);
            movies.add(movie);
        }
    }

    @Override
    public String toString() {
        return movies.toString();
    }

    @Override
    public Iterator<Movie> iterator() {
        return movies.iterator();
    }
}
