package abacus.demo;

import abacus.api.AbacusRequest;
import abacus.api.AbacusResult;
import abacus.search.facets.FastDocValuesAtomicReader;
import abacus.service.AbacusQueryParser;
import abacus.service.AbacusQueryService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.File;
import java.io.IOException;

import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

public class Application {
  private static final AbacusRequest convert(Request req) {
    return null;
  }

  private static final String convert(AbacusResult result) {
    return null;
  }

  public static void main(String[] args) throws Exception {

    File idxDir = new File(args[0]);

    Directory fsDir = FSDirectory.open(idxDir);

    final AbacusQueryService svc = new AbacusQueryService(fsDir,
        new AbacusQueryParser.DefaultQueryParser("contents",
        new StandardAnalyzer(Version.LUCENE_48)),
        null, FastDocValuesAtomicReader.MemType.Heap
    );

    // Will serve all static file are under "/public" in classpath if the route isn't consumed by others routes.
    // When using Maven, the "/public" folder is assumed to be in "/main/resources"
    staticFileLocation("/public");

    post(new Route("/search", "application/json") {
      @Override
      public Object handle(Request req, Response resp) {
        AbacusResult rs;
        try {
          rs = svc.query(convert(req));
        } catch (Exception e) {
          e.printStackTrace();
          rs = new AbacusResult();
          rs.setLatencyInMs(0L);
          rs.setNumHits(0L);
        }

        System.out.println("request took: " + rs.getLatencyInMs() + "ms");
        return convert(rs);
      }

    });

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        try {
          svc.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }
}
