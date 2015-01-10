/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package movie.genre;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static movie.genre.Movie.Genre.*;
import static movie.genre.MovieClassifier.TOKENIZER;
import movie.genre.pmm.DirFileWriter;
import movie.genre.view.RootView;
import org.json.JSONException;
import org.json.JSONObject;
import org.tartarus.snowballm.EnglishStemmer;

/**
 *
 * @author Olayinka
 */
public class Movie {

    private static final Genre[] GENRES_LIST = new Genre[]{ACTION, ADVENTURE, COMEDY, CRIME, DOCUMENTARY, DRAMA, FAMILY, FANTASY, HORROR, MYSTERY, ROMANCE, SCI_FI, THRILLER};
    private static final HashMap<String, Genre> GENRE_MAP;
    private static List<Movie> MOVIES;
    static double[] GENRES_FREQ = new double[1 << Genre.values().length];
    private static MovieClassifier CLASSIFIER;
    private static double[][] THETA;

    static {
        GENRE_MAP = new HashMap<>(GENRES_LIST.length);
        for (Genre genre : GENRES_LIST) {
            GENRE_MAP.put(genre.toString(), genre);
        }
    }

    public static double[] getGenresFreq() {
        return GENRES_FREQ;
    }

    public static Genre[] getGenresList() {
        return GENRES_LIST;
    }

    public static List<Movie> getMovies() {
        return MOVIES;
    }

    public static HashMap<String, Genre> getGenreMap() {
        return GENRE_MAP;
    }

    public static void setMovies(MovieClassifier parser, RootView rootView) throws IOException, JSONException {
        String log = "Setting movies : ";
        CLASSIFIER = parser;
        BufferedReader reader = new BufferedReader(new FileReader(MovieClassifier.TEST_MOVIES_SRC));
        String line;
        List<String> wordList = CLASSIFIER.getDictionary().getWordList();
        THETA = new double[wordList.size()][GENRES_LIST.length];
        for (int w = 0; w < wordList.size(); w++) {
            for (int g = 0; g < GENRES_LIST.length; g++) {
                THETA[w][g] = 1;
            }
        }
        MOVIES = new ArrayList<>(100000);
        int i = 0;
        while ((line = reader.readLine()) != null) {
            i++;
            JSONObject object = new JSONObject(line);
            MOVIES.add(new Movie(object));
            String text = log + i;
            rootView.onChangeAnalysis(text);
        }
        for (i = MOVIES.size() - 1; i >= 0; i--) {
            if (MOVIES.get(i).isFlagged()) {
                MOVIES.remove(i);
            }
        }

        for (Movie movie : MOVIES) {
            GENRES_FREQ[movie.getGenre()]++;
        }
        double chk = 0.0;
        for (i = 0; i < GENRES_FREQ.length; i++) {
            GENRES_FREQ[i] /= MOVIES.size();
            chk += GENRES_FREQ[i];
        }
        assert chk - 1 < 1e-9;

        for (int g = 0; g < GENRES_LIST.length; g++) {
            double sum = 0.0;
            for (int w = 0; w < wordList.size(); w++) {
                sum += THETA[w][g];
            }
            for (int w = 0; w < wordList.size(); w++) {
                THETA[w][g] /= sum;
            }
        }
    }

    public static void setMoviesTdIdf(MovieClassifier parser) {
        int i = 0;
        for (Movie movie : MOVIES) {
            i++;
            movie.setWordFrequencyTdIdf();
            System.out.println("Setting td-idf: " + i);
        }
        System.out.println("");
    }

    public static void exportWordFrequency() throws IOException {
        for (Movie movie : MOVIES) {
            try (BufferedWriter writer = new BufferedWriter(new DirFileWriter(movie.getBowFileName()))) {
                for (Map.Entry<Integer, WordFrequency> entry : movie.getWordFrequency().entrySet()) {
                    writer.write(entry.getKey() + "," + entry.getValue().getRaw() + "," + entry.getValue().getTfIdf());
                    writer.newLine();
                }
                writer.flush();
                writer.close();
            }
        }
    }

    public static boolean hasGenres(JSONObject object) throws JSONException {
        String[] tGenres = object.getString("genres").split(",\\s+");
        Genre[] genres = new Genre[tGenres.length];
        for (int i = 0; i < tGenres.length; i++) {
            genres[i] = GENRE_MAP.get(tGenres[i]);
            if (genres[i] != null) {
                return true;
            }
        }
        return false;
    }

    public static Genre[] getGenre(JSONObject object) throws JSONException {
        String[] tGenres = object.getString("genres").split(",\\s+");
        Genre[] genres = new Genre[tGenres.length];
        for (int i = 0; i < tGenres.length; i++) {
            genres[i] = GENRE_MAP.get(tGenres[i]);
        }
        return genres;
    }

    public static double[][] getTheta() {
        return THETA;
    }

    TreeMap<Integer, WordFrequency> wordFrequency;
    String synopsis;
    String title;
    String actors;
    String director;
    String year;
    double[] genres;
    int genre = 0;
    String imdbId;

    boolean flag = false;

    public Movie(JSONObject jsonObject) throws JSONException {
        synopsis = jsonObject.getString("synopsis");
        title = jsonObject.getString("title");
        actors = jsonObject.getString("actors");
        director = jsonObject.getString("director");
        String tGenres = jsonObject.getString("genres");
        genres = new double[GENRES_LIST.length];
        double numGenre = 0;
        for (int i = 0; i < genres.length; i++) {
            genres[i] = tGenres.contains(GENRES_LIST[i].toString()) ? 1.0 : 0.0;
            if (genres[i] != 0.0) {
                genre |= 1 << i;
            }
            numGenre += genres[i];
        }
        for (int i = 0; i < genres.length; i++) {
            genres[i] = genres[i] / numGenre;
        }
        imdbId = jsonObject.getString("imdbId");
        setWordFrequencyRaw();
    }

    public int getGenre() {
        return genre;
    }

    public Movie(String title, String actors, String director, String summary) {
        this.title = title;
        this.director = director;
        this.synopsis = summary;
        this.actors = actors;
        flag = true;
        setWordFrequencyRaw();
    }

    void flag() {
        flag = true;
    }

    boolean isFlagged() {
        return flag;
    }

    public double[] getGenres() {
        return genres;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getTitle() {
        return title;
    }

    public String getImdbId() {
        return imdbId;
    }

    public String getActors() {
        return actors;
    }

    public String getDirector() {
        return director;
    }

    public String getYear() {
        return year;
    }

    public TreeMap<Integer, WordFrequency> getWordFrequency() {
        return wordFrequency;
    }

    private void setWordFrequencyRaw() {
        List<String> wordList = CLASSIFIER.getDictionary().getWordList();
        wordFrequency = new TreeMap<>();
        String[] tokens = TOKENIZER.tokenize(synopsis);
        for (int i = 0; i < wordList.size(); i++) {
            int freq = 0;
            for (String token : tokens) {
                token = EnglishStemmer.instance().stem(token.trim().toLowerCase());
                freq += token.equals(wordList.get(i)) ? 1 : 0;

            }
            if (!flag) {
                for (int g = 0; g < genres.length; g++) {
                    if (genres[g] > 1e-12) {
                        //System.out.println(genres[g]);
                        THETA[i][g] += freq;
                    }
                }
            }
            if (freq > 0) {
                WordFrequency wordFreq = new WordFrequency();
                wordFreq.setRawFrequency(freq);
                wordFrequency.put(i, wordFreq);
            }
        }
    }

    public void setWordFrequencyTdIdf() {
        List<String> wordList = CLASSIFIER.getDictionary().getWordList();
        int[] dictFreq = CLASSIFIER.getDictionary().getWordFrequency();

        double modulo = 0.0, verify = 0.0;
        for (Map.Entry<Integer, WordFrequency> entry : wordFrequency.entrySet()) {
            double tfIdf = (1.0 * entry.getValue().getRaw()) * Math.log((1.0 * MOVIES.size()) / dictFreq[entry.getKey()]);
            entry.getValue().setTfIdf(tfIdf);
            modulo += Math.pow(tfIdf, 2);
        }
        if (Math.abs(modulo) < 1e-9) {
            flag();
            return;
        }
        modulo = Math.sqrt(modulo);
        for (Map.Entry<Integer, WordFrequency> entry : wordFrequency.entrySet()) {
            entry.getValue().setTfIdf(entry.getValue().getTfIdf() / modulo);
            verify += Math.pow(entry.getValue().getTfIdf(), 2);
        }
        assert Math.abs(verify - 1.0) < 1e-9;
    }

    public String getBowFileName() {
        return "resources/bows/" + imdbId + ".bow";
    }

    public double getGenreValue(int g) {
        return genres[g];
    }

    public static enum Genre {

        ACTION, ADVENTURE, COMEDY, CRIME, DOCUMENTARY, DRAMA, FAMILY, FANTASY, HORROR, MYSTERY, ROMANCE, SCI_FI, THRILLER;

        @Override
        public String toString() {
            if (this == SCI_FI) {
                return "Sci-Fi";
            }
            String ret = super.toString();
            return ret.substring(0, 1) + ret.substring(1).toLowerCase();
        }
    }

    public static class WordFrequency {

        private int raw;
        private double tfIdf;

        void setRawFrequency(int rawFrequency) {
            this.raw = rawFrequency;
        }

        void setTfIdf(double tfIdf) {
            this.tfIdf = tfIdf;
        }

        public int getRaw() {
            return raw;
        }

        public double getTfIdf() {
            return tfIdf;
        }

    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getTitle());
        builder.append("\n");
        builder.append(getActors());
        builder.append("\n");
        builder.append(getDirector());
        builder.append("\n");
        builder.append(getSynopsis());
        builder.append("\n");
        return builder.toString();
    }

}
