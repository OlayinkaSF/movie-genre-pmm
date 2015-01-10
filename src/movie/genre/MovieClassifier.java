/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package movie.genre;

import movie.genre.knn.KNNClassifier;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import movie.genre.knn.TrainingSet;
import movie.genre.pmm.Dictionary;
import movie.genre.pmm.PMMClassifier;
import movie.genre.view.RootView;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Olayinka
 */
public final class MovieClassifier {

    private static String GENRES_DIR = "resources/genres/";
    public static int MIN_SYN_LENGTH = 250;
    public static final String IMDB_MOVIES_SRC = "resources/imdb-movies.list";
    public static final String MOVIES_SRC = "resources/movies.json";
    public static Tokenizer TOKENIZER = null;

    /**
     * JSON text file containing movie titles and year of production This is
     * parsed into a JSON objects and used to pull data from IMPD
     *
     */
    public static final String TRAINING_MOVIES_SRC = "resources/training.json";

    public static final String TEST_MOVIES_SRC = "resources/training-min.json";

    static final int LIMIT_MOVIES_SIZE = 50000;

    static {
        InputStream modelIn = null;
        try {
            TokenizerModel model;
            modelIn = new FileInputStream("resources/en-token.bin");
            model = new TokenizerModel(modelIn);
            TOKENIZER = new TokenizerME(model);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(MovieClassifier.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MovieClassifier.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                modelIn.close();
            } catch (IOException ex) {
                Logger.getLogger(MovieClassifier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void parseRawImdbData() throws IOException {
        HashSet<String> movies;
        try (BufferedReader reader = new BufferedReader(new FileReader(IMDB_MOVIES_SRC))) {
            movies = new HashSet<>(2000000);
            String line;
            while ((line = reader.readLine()) != null) {
                int pos = line.indexOf("("), year = -1;
                while (pos != -1) {
                    try {
                        String yearString = line.substring(pos, pos + 6);
                        yearString = yearString.substring(1, 5);
                        year = Integer.parseInt(yearString);
                        break;
                    } catch (NumberFormatException ex) {
                        pos = line.indexOf("(", pos + 1);
                    } catch (StringIndexOutOfBoundsException ex) {
                        break;
                    }
                }
                if (year == -1) {
                    continue;
                }
                String title = line.substring(0, pos).trim();
                if (title.charAt(0) == '\"') {
                    title = title.substring(1, title.length() - 1);
                }
                title = title.replaceAll("\"", "");
                JSONObject object = new JSONObject();
                try {
                    object.put("title", title);
                    object.put("year", year);
                    movies.add(object.toString());
                } catch (JSONException ex) {
                    Logger.getLogger(MovieClassifier.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MOVIES_SRC))) {
            for (String string : movies) {
                writer.write(string);
                writer.newLine();
            }
        }
    }

    public static void getDataFromImdbApi() throws IOException, JSONException {

        String[][] fields = new String[][]{
            {"title", "Title"},
            {"actors", "Actors"},
            {"director", "Director"},
            {"synopsis", "Plot"},
            {"genres", "Genre"},
            {"year", "Year"},
            {"imdbId", "imdbID"}
        };

        try (BufferedReader reader = new BufferedReader(new FileReader(MOVIES_SRC))) {

            String line;
            int limit = 0;
            int num = 0;

            while ((line = reader.readLine()) != null && !line.equals("{\"title\":\"Kicks\",\"year\":2009}")) {
                num++;
            }
            String[] filterNo = new String[]{"Action", "Comedy", "Crime", "Documentary", "Drama", "Horror", "Romance", "Thriller"};

            String[] filterYes = new String[]{"Adventure", "Family", "Mystery", "Sci-Fi", "Fantasy"};

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TRAINING_MOVIES_SRC, true))) {
                //writer.write("[");
                //writer.newLine();
                while ((line = reader.readLine()) != null) {
                    System.out.println(++num);
                    String url = "http://www.omdbapi.com/";
                    String charset = StandardCharsets.UTF_8.name();

                    JSONObject object = new JSONObject(line);

                    String query = String.format("t=%s&y=%s&plot=full&r=json",
                            URLEncoder.encode(object.getString("title"), charset),
                            URLEncoder.encode(object.getString("year"), charset)
                    );

                    InputStream response = null;
                    boolean res = false;
                    while (!res) {
                        try {
                            URLConnection connection = new URL(url + "?" + query).openConnection();
                            connection.setRequestProperty("Accept-Charset", charset);
                            response = connection.getInputStream();
                            res = true;
                        } catch (Exception e) {
                            System.out.println(url + "?" + query);
                        }
                    }

                    BufferedReader responseReader = new BufferedReader(new InputStreamReader(response));
                    JSONObject responseObject;
                    try {
                        responseObject = new JSONObject(responseReader.readLine());
                    } catch (JSONException ex) {
                        continue;
                    }

                    String responseValue = responseObject.getString("Response");
                    if (responseValue.equals("False")) {
                        continue;
                    }

                    boolean failFlag = true;
                    responseValue = responseObject.getString("Genre");
                    for (String string : filterYes) {
                        if (responseValue.contains(string)) {
                            failFlag = false;
                            break;
                        }
                    }
                    responseValue = responseObject.getString("Plot");
                    if (failFlag || responseValue.length() < MIN_SYN_LENGTH) {
                        continue;
                    }
                    JSONObject movie = new JSONObject();
                    for (String[] field : fields) {
                        if (responseObject.getString(field[1]).trim().equals("N/A")) {
                            failFlag = true;
                            break;
                        }
                        movie.put(field[0], responseObject.get(field[1]));
                    }

                    if (failFlag || !responseObject.getString("Language").equals("English")) {
                        continue;
                    }
                    limit++;
                    writer.write(movie.toString() + (limit == LIMIT_MOVIES_SIZE ? "" : ","));
                    writer.newLine();
                    writer.flush();
                    if (limit == LIMIT_MOVIES_SIZE) {
                        break;
                    }
                    System.out.println("-----------------------" + limit + "--------------------------");
                }
                //writer.write("]");

            }
        }
    }

    public static void main(String[] args) throws IOException, JSONException {
        //parseRawImdbData();
        //getDataFromImdbApi();
        final RootView rootView = new RootView();
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(RootView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                rootView.setVisible(true);
            }
        });
        MovieClassifier classifier = new MovieClassifier(rootView);
        classifier.execute();
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return (directory.delete());
    }
    private Dictionary dictionary;
    RootView rootView;
    PMMClassifier pmmClassifier;
    private KNNClassifier knnClassifier;

    public MovieClassifier(RootView rootView) {
        this.rootView = rootView;
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    private void execute() throws IOException, JSONException {
        dictionary = new Dictionary();
        dictionary.createWordList(rootView);
        dictionary.setWordList();
        Movie.setMovies(this, rootView);
        dictionary.setWordFrequency(rootView);
        //Movie.setMoviesTdIdf(parser);
        //Movie.exportWordFrequency();
        pmmClassifier = new PMMClassifier(Movie.getTheta(), this);
        pmmClassifier.train(rootView);
        TrainingSet trainingSet = new TrainingSet();
        //pmmClassifier.test();
        knnClassifier = new KNNClassifier(trainingSet);
        rootView.setKnnClassifier(knnClassifier);
        rootView.setPmmClassifier(pmmClassifier);
        rootView.onFinishAnalysis();
    }

}
