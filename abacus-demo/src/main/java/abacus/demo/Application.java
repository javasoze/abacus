package abacus.demo;

import static spark.Spark.get;
import static spark.Spark.staticFileLocation;
import spark.Request;
import spark.Response;
import spark.Route;

public class Application {

	public static void main(String[] args) {
	    // Will serve all static file are under "/public" in classpath if the route isn't consumed by others routes.
	    // When using Maven, the "/public" folder is assumed to be in "/main/resources"
	    staticFileLocation("/public");
	
	    get(new Route("/hello") {
	        @Override
	        public Object handle(Request request, Response response) {
	            return "Hello World!";
	        }
	    });
	}
}
