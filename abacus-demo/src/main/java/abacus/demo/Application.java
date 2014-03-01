package abacus.demo;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

import java.io.File;

import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;

import spark.Request;
import spark.Response;
import spark.Route;

public class Application {

	public static void main(String[] args) throws Exception {
	  
	  File idxDir = new File(args[0]);
	  
	  DirectoryReader reader = DirectoryReader.open(FSDirectory.open(idxDir));
	  
	  SortedSetDocValuesReaderState facetState = new DefaultSortedSetDocValuesReaderState(reader);
	  
    // Will serve all static file are under "/public" in classpath if the route isn't consumed by others routes.
    // When using Maven, the "/public" folder is assumed to be in "/main/resources"
    staticFileLocation("/public");

    get(new Route("/hello") {
        @Override
        public Object handle(Request request, Response response) {
            return "Hello World!";
        }
    });
    
    post(new Route("/search", "application/json") {
		@Override
  		public Object handle(Request req, Response resp) {
  			String reqBody = req.body();
  			System.out.println(reqBody);
  			
  			return "{\"numhits\":123,\"totaldocs\":123,\"time\":123,\"hits\":[],\"facets\":{}}";
  		}
    	
    });
    
    reader.close();
	}
}
