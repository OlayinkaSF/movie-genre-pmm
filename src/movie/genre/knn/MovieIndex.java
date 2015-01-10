package movie.genre.knn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import movie.genre.Movie;

public class MovieIndex {

    public static final String DIRECTOR = "director";
    public static final String ACTORS = "actors";
    public static final String TITLE = "title";
    public static final String SYNOPSIS = "synopsis";

    private final List<Movie> indexedMovies;
    private final IndexWriter writer;

    public MovieIndex() throws IOException {
        final RAMDirectory indexDirectory = new RAMDirectory();
        final IndexWriterConfig config = new IndexWriterConfig(
                Version.LATEST, new StandardAnalyzer());
        writer = new IndexWriter(indexDirectory, config);

        indexedMovies = new ArrayList<>();
    }

    public void index(final Movie movie) {
        final Document document = new Document();
        document.add(new TextField(TITLE, movie.getTitle(),
                Field.Store.YES));
        document.add(new TextField(ACTORS, movie.getActors(),
                Field.Store.YES));
        document.add(new TextField(DIRECTOR, movie.getDirector(),
                Field.Store.YES));
        document.add(new TextField(SYNOPSIS, movie.getSynopsis(),
                Field.Store.YES));
        try {
            writer.addDocument(document);
            indexedMovies.add(movie);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Movie, Float> getNeighbours(final Movie movie,
            final int numberOfHits) throws ParseException, IOException {
        final StandardAnalyzer analyzer = new StandardAnalyzer();
        final QueryParser parser = new QueryParser(SYNOPSIS, analyzer);
        final Query query = parser.parse(movie.toString());
        final TopScoreDocCollector collector = TopScoreDocCollector.create(
                numberOfHits, true);

        final IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(
                writer, false));
        searcher.search(query, collector);

        final Map<Movie, Float> result = new LinkedHashMap<>();
        final ScoreDoc[] scoreDocs = collector.topDocs().scoreDocs;
        for (final ScoreDoc doc : scoreDocs) {
            result.put(indexedMovies.get(doc.doc), 1 - doc.score);
        }
        return result;
    }
}
