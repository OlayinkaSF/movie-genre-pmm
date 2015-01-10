/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package movie.genre.pmm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import movie.genre.Movie;
import movie.genre.MovieClassifier;
import static movie.genre.MovieClassifier.MIN_SYN_LENGTH;
import static movie.genre.MovieClassifier.TEST_MOVIES_SRC;
import static movie.genre.MovieClassifier.TOKENIZER;
import movie.genre.view.RootView;
import org.json.JSONException;
import org.json.JSONObject;
import org.tartarus.snowballm.EnglishStemmer;

/**
 *
 * @author Olayinka
 */
public class Dictionary {

    /**
     * Plots pulled from training movies are used to build a dictionary.
     */
    public static final String DICTIONARY_SRC = "resources/dictionary.list";
    private static final String STOP_WORDS_SRC = "resources/stop-words.list";
    static final int LIMIT_FREQ = 10;

    private int[] wordFrequency;
    private HashSet<String> stopWords;
    private List<String> wordList;

    public void setWordList() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(DICTIONARY_SRC));
        wordList = new ArrayList<>(20000);
        String line;
        while ((line = reader.readLine()) != null) {
            wordList.add(line);
        }
    }

    public List<String> getWordList() {
        return wordList;
    }

    public int[] getWordFrequency() {
        return wordFrequency;
    }

    public void setWordFrequency(RootView rootView) {
        String log = "Setting word frequency: ";
        wordFrequency = new int[wordList.size()];
        List<Movie> movies = Movie.getMovies();
        for (int i = 0; i < wordList.size(); i++) {
            int sum = 0;
            for (Movie movie : movies) {
                if (movie.getWordFrequency().containsKey(i)) {
                    sum += (movie.getWordFrequency().get(i).getRaw() > 0 ? 1 : 0);
                }
            }
            wordFrequency[i] = sum;
            String text = log + i;
            rootView.onChangeAnalysis(text);
        }
        System.out.println("");

    }

    public void createWordList(RootView rootView) throws IOException, JSONException {
        String log = "Creating word list...";
        rootView.onChangeAnalysis(log);
        String line;
        HashMap<String, Integer> words = new HashMap<>(1000000);
        HashMap<Movie.Genre, Integer> genreFreq = new HashMap<>(Movie.Genre.values().length);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_MOVIES_SRC))) {
            writer.write("");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DICTIONARY_SRC))) {
            writer.write("");
        }

        stopWords = new HashSet<>(1000);
        BufferedReader stopWordsReader = new BufferedReader(new FileReader(STOP_WORDS_SRC));
        while ((line = stopWordsReader.readLine()) != null) {
            stopWords.add(line.trim());
        }

        int num = 0;
        BufferedReader reader = new BufferedReader(new FileReader(MovieClassifier.TRAINING_MOVIES_SRC));
        while ((line = reader.readLine()) != null) {

            rootView.onChangeAnalysis(log);

            JSONObject object = new JSONObject(line);
            String synopsis = object.getString("synopsis");
            String genreChk = object.getString("genres");

            if (synopsis.length() < MIN_SYN_LENGTH
                    || !Movie.hasGenres(object)
                    || genreChk.equals("Drama")
                    || genreChk.equals("Comedy")
                    || genreChk.equals("Comedy, Drama")
                    || genreChk.equals("Drama, Comedy")) {
                continue;
            }

            Movie.Genre[] genres = Movie.getGenre(object);
            for (Movie.Genre genre : genres) {
                if (genre != null) {
                    Integer freq = genreFreq.get(genre);
                    freq = freq == null ? 0 : freq;
                    freq = freq + 1;

                    genreFreq.put(genre, freq);
                }
            }

            num++;
            String[] tokenned = TOKENIZER.tokenize(synopsis);
            for (String token : tokenned) {
                token = token.toLowerCase().trim();
                if (token.length() > 2 && !stopWords.contains(token) && !token.matches("^[0-9]+$")) {
                    token = EnglishStemmer.instance().stem(token);
                    Integer freq = words.get(token);
                    if (freq == null) {
                        freq = 0;
                    }
                    freq = freq + 1;
                    words.put(token, freq);
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEST_MOVIES_SRC, true))) {
                writer.write(object.toString());
                writer.newLine();
                writer.flush();
            }

            if (num == 200) {
                break;
            }

        }

        System.out.println(num);
        for (Map.Entry<Movie.Genre, Integer> entry : genreFreq.entrySet()) {
            System.out.println(entry.getKey().toString() + "\t" + entry.getValue() * 1.0 / num);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DICTIONARY_SRC))) {
            for (Map.Entry<String, Integer> entry : words.entrySet()) {
                if (entry.getValue().compareTo(LIMIT_FREQ) > 0) {
                    writer.write(entry.getKey());
                    writer.newLine();
                }
            }
        }
    }

}
