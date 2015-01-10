/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package movie.genre.pmm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import movie.genre.Movie;
import movie.genre.Movie.Genre;
import movie.genre.MovieClassifier;
import movie.genre.view.RootView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Olayinka
 */
public class PMMClassifier {

    public static final String TEST_SRC = "classify.json";
    public double[][] theta;
    MovieClassifier classifier;

    public PMMClassifier(double[][] theta, MovieClassifier classifier) {
        this.theta = theta;
        this.classifier = classifier;
    }

    /**
     * l represent the index of a genre and w represent the index of a word
     *
     * @param rootView
     */
    public void train(RootView rootView) {
        String log = "Training Data sets";
        List<String> wordList = classifier.getDictionary().getWordList();
        Genre[] genres = Movie.getGenresList();
        System.out.println(genres.length * wordList.size());
        System.out.println(Arrays.deepToString(theta));
        boolean converge = false;
        while (!converge) {
            log += ".";
            log = log.substring(0, Math.min(log.length(), 36));
            rootView.onChangeAnalysis(log);
            converge = true;
            double[][] thetaN = theta(theta);
            for (int w = 0; w < theta.length && converge; w++) {
                for (int g = 0; g < theta[0].length && converge; g++) {
                    double diff = Math.abs(theta[w][g] - thetaN[w][g]);
                    if (diff > 1e-15) {
                        converge = false;
                        System.out.println(theta[w][g] - thetaN[w][g]);
                    }
                }
            }
            theta = thetaN;
        }
    }

    double[][] theta(double[][] theta) {
        List<Movie> movies = Movie.getMovies();
        List<String> wordList = classifier.getDictionary().getWordList();
        Genre[] genres = Movie.getGenresList();

        double[][][] gammas = new double[movies.size()][wordList.size()][genres.length];
        double[][] thetaN = new double[wordList.size()][genres.length];

        for (int m = 0; m < movies.size(); m++) {
            Movie movie = movies.get(m);
            for (int w = 0; w < wordList.size(); w++) {
                for (int g = 0; g < genres.length; g++) {
                    double num, den = 0.0;
                    num = movie.getGenres()[g] * theta[w][g];
                    for (int i = 0; i < genres.length; i++) {
                        den += movie.getGenres()[i] * theta[w][i];

                    }
                    gammas[m][w][g] = num / den;
                }
            }
        }

        for (int w = 0; w < wordList.size(); w++) {
            for (int g = 0; g < genres.length; g++) {
                double num = 0.0, den = 0.0;
                for (int m = 0; m < movies.size(); m++) {
                    Movie.WordFrequency freq = movies.get(m).getWordFrequency().get(w);
                    num += (freq == null ? 0.0 : freq.getRaw()) * gammas[m][w][g];
                }
                for (int ww = 0; ww < wordList.size(); ww++) {
                    for (int m = 0; m < movies.size(); m++) {
                        Movie.WordFrequency freq = movies.get(m).getWordFrequency().get(ww);
                        den += (freq == null ? 0.0 : freq.getRaw()) * gammas[m][ww][g];
                    }
                }
                thetaN[w][g] = (num + 1) / (den + wordList.size());
            }
        }
        return thetaN;
    }

    public void test() throws IOException, JSONException {
        StringBuilder builder = new StringBuilder();
        String line;
        try (BufferedReader read = new BufferedReader(new FileReader("resources/" + TEST_SRC))) {
            while ((line = read.readLine()) != null) {
                builder.append(line);
            }
        }
        JSONArray jSONArray = new JSONArray(builder.toString());
        for (int i = 0; i < jSONArray.length(); i++) {
            JSONObject object = jSONArray.getJSONObject(i);
            classify(object);
        }
    }

    private String classify(JSONObject object) throws JSONException {
        Movie movie = new Movie(object);
        return classify(movie);
    }

    public String classify(Movie movie) {
        int genre = 0;
        double max = 0.0;
        List<Movie> movies = Movie.getMovies();
        List<String> wordList = classifier.getDictionary().getWordList();
        Genre[] genres = Movie.getGenresList();
        //movie.setWordFrequencyTdIdf();
        for (int i = 1; i < (1 << Genre.values().length); i++) {
            int tot = countSetBits(i);
            double den = 0.0;
            for (int g = 0; g < genres.length; g++) {
                den += ((((1 << g) & i) > 0 ? 1.0 : 0.0) / tot);
            }
            double prod = 1.0;
            for (int w = 0; w < wordList.size(); w++) {
                double num = 0.0;
                for (int g = 0; g < genres.length; g++) {
                    num += ((((1 << g) & i) > 0 ? 1.0 : 0.0) / tot) * theta[w][g];
                }
                Movie.WordFrequency freq = movie.getWordFrequency().get(w);
                prod *= Math.pow(num / den, (freq == null ? 0.0 : freq.getRaw()));
            }
            prod *= Movie.getGenresFreq()[i];
            if (prod > max) {
                max = prod;
                genre = i;
            }
        }

        System.out.println(movie.getTitle());
        List<String> result = new ArrayList<>(genres.length);

        for (int g = 0; g < genres.length; g++) {
            if (((1 << g) & genre) > 0) {
                result.add(genres[g].toString());
            }
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < result.size(); i++) {
            builder.append(result.get(i));
            if (i < result.size() - 1) {
                builder.append(", ");
            }
        }
        System.out.println(builder.toString());
        System.out.println();
        return builder.toString();
    }

    static int countSetBits(int n) {
        int count = 0;
        while (n > 0) {
            count += n & 1;
            n >>= 1;
        }
        return count;
    }
}
