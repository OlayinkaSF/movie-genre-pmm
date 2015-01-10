package movie.genre.knn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.lucene.queryparser.classic.ParseException;

import movie.genre.Movie;
import movie.genre.Movie.Genre;

public class KNNClassifier {

    private static final int NEIGHBOURS_COUNT = 3;

    private MovieIndex index;

    public KNNClassifier(final TrainingSet trainingSet) {
        try {
            index = new MovieIndex();
            for (final Movie movie : trainingSet) {
                index.index(movie);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String classify(final Movie movie) {
        try {
            Map<Movie, Float> neighbours = index.getNeighbours(movie,
                    NEIGHBOURS_COUNT);
            return getResult(computeGenreValues(neighbours));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Map<Genre, Float> computeGenreValues(Map<Movie, Float> neighbours) {
        final Map<Genre, Float> classification = new HashMap<>();

        float nominator = 0f;
        float denominator = 0f;
        float total = 0f;

        for (int g = 0; g < Movie.getGenresList().length; g++) {
            Genre genre = Movie.getGenresList()[g];
            for (final Entry<Movie, Float> entry : neighbours.entrySet()) {
                final Movie movie = entry.getKey();
                final float score = entry.getValue();
                nominator += movie.getGenreValue(g) * (1 / (score * score));
                denominator += (1 / (score * score));
            }

            final float value = denominator != 0 ? nominator / denominator : 0f;
            total += value;
            classification.put(genre, value);
        }

        if (total != 0) {
            final float coef = 1 / total;
            for (final Entry<Genre, Float> entry : classification.entrySet()) {
                classification.put(entry.getKey(), entry.getValue() * coef);
            }
        }

        return classification;
    }

    private String getResult(Map<Genre, Float> genreValues) {
        ArrayList<GenreResult> results = new ArrayList<>(genreValues.size());
        System.out.println(genreValues.values());
        for (Entry<Genre, Float> entry : genreValues.entrySet()) {
            results.add(new GenreResult(entry.getKey(), entry.getValue()));
        }
        Collections.sort(results);
        Collections.reverse(results);
        StringBuilder builder = new StringBuilder();
        builder.append(results.get(0).genre.toString());
        builder.append(", ");
        builder.append(results.get(1).genre.toString());
        return builder.toString();
    }

    class GenreResult implements Comparable<GenreResult> {

        Genre genre;
        Float res;

        public GenreResult(Genre genre, Float res) {
            this.genre = genre;
            this.res = res;
        }

        @Override
        public int compareTo(GenreResult o) {
            return res.compareTo(o.res);
        }

    }
}
