package abacus.demo;

import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONArray;
import org.json.JSONObject;

import spark.Request;
import spark.Response;
import spark.Route;
import abacus.api.query.Facet;
import abacus.api.query.FacetParam;
import abacus.api.query.ResultSet;
import abacus.api.query.Selection;
import abacus.service.AbacusQueryService;
import abacus.service.QueryParser;

public class Application {

  private static final int DEFAUT_MAX_FACET_VALS = 100;
  private static final abacus.api.query.Request convert(Request req) {
    String reqBody = req.body();
    System.out.println(reqBody);
    JSONObject json = new JSONObject(reqBody);
    // get query
    String qString = null;
    JSONObject queryObj = json.optJSONObject("query");
    if (queryObj != null) {
      JSONObject qstringObj = queryObj.optJSONObject("query_string");
      if (qstringObj != null) {
        qString = qstringObj.optString("query");
      }
    }
    
    abacus.api.query.Request abacusReq = new abacus.api.query.Request();
    if (qString != null && !(qString = qString.trim()).isEmpty()) {
      abacusReq.setQueryString(qString);
    }
    
    // get the selections
    JSONArray selArray = json.optJSONArray("selections");
    if (selArray != null) {
      Map<String, List<Selection>> selectionMap = new HashMap<String, List<Selection>>();
      abacusReq.setSelections(selectionMap);
      for (int i = 0; i < selArray.length(); ++i) {
        JSONObject selObj = selArray.getJSONObject(i);
        JSONObject selection = selObj.optJSONObject("terms");
        if (selection == null) {
          selection = selObj.optJSONObject("path");
        }
        if (selection == null) {
          continue;
        }
        for (String key : (Set<String>) selection.keySet()) {
          JSONObject obj = selection.getJSONObject(key);
          
          List<Selection> selList = selectionMap.get(key);
          if (selList == null) {
            selList = new ArrayList<Selection>();
            selectionMap.put(key, selList);
          }
          
          String selLabel = obj.optString("value");
          if (selLabel != null) {
            Selection s = new Selection();
            s.setValues(Arrays.asList(selLabel));
            selList.add(s);
          } else {
            JSONArray selVals = obj.optJSONArray("values");
            
            if (selVals.length() > 0) {            
              List<String> selLabels = new ArrayList<String>();
              for (int k = 0; k < selVals.length(); ++k) {
                selLabels.add(selVals.getString(k));        
              }
              Selection s = new Selection();
              s.setValues(selLabels);
              selList.add(s);
            }
          }
        }
      }
    }
    
    // get facet settings
    JSONObject facetObj = json.optJSONObject("facets");
    if (facetObj != null) {
      Map<String, FacetParam> facetParamMap = new HashMap<String, FacetParam>();
      abacusReq.setFacetParams(facetParamMap);
      for (String key : (Set<String>) facetObj.keySet()) {
        JSONObject perDimObj = facetObj.optJSONObject(key);
        if (perDimObj != null) {
          FacetParam facetParam = new FacetParam();
          facetParam.setDrillSideways(perDimObj.optBoolean("expand"));
          facetParam.setMinCount(1);
          facetParam.setMaxNumValues(DEFAUT_MAX_FACET_VALS);
          facetParamMap.put(key, facetParam);
        }
      }
    }
    return abacusReq;
  }
  
  private static final String convert(ResultSet resultSet) {
    JSONObject respObj = new JSONObject();
    respObj.put("numhits", resultSet.getNumHits());
    respObj.put("totaldocs", resultSet.getCorpusSize());
    respObj.put("time", resultSet.getLatencyInMs());
    Map<String, List<Facet>> facetMap = resultSet.getFacetList();
    if (facetMap != null) {
      JSONObject facetObj = new JSONObject();
      respObj.put("facets", facetObj);
      for (Entry<String, List<Facet>> entry : facetMap.entrySet()) {
        JSONArray perDimFacetArr = new JSONArray();
        facetObj.put(entry.getKey(), perDimFacetArr);
        for (Facet f : entry.getValue()) {
          JSONObject perDimFacet = new JSONObject();
          perDimFacet.put("value", f.getValue());
          perDimFacet.put("count", f.getCount());
          perDimFacetArr.put(perDimFacet);
        }
      }
    }
    return respObj.toString();
  }
  
	public static void main(String[] args) throws Exception {
	  
	  File idxDir = new File(args[0]);
	  
	  final AbacusQueryService svc = new AbacusQueryService(FSDirectory.open(idxDir), 
	      new QueryParser.DefaultQueryParser("contents", new StandardAnalyzer(Version.LUCENE_48)));	  
	  
    // Will serve all static file are under "/public" in classpath if the route isn't consumed by others routes.
    // When using Maven, the "/public" folder is assumed to be in "/main/resources"
    staticFileLocation("/public");
    
    post(new Route("/search", "application/json") {
		@Override
  		public Object handle(Request req, Response resp) {
		  ResultSet rs;
  			try {
           rs = svc.query(convert(req));
        } catch (Exception e) {
          e.printStackTrace();
          rs = new ResultSet();
          rs.setLatencyInMs(0L);
          rs.setNumHits(0L);
        }
  			
  			return convert(rs);
  		}
    	
    });
    
    Runtime.getRuntime().addShutdownHook(new Thread(){
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
