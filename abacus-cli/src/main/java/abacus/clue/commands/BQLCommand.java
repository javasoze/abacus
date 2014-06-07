package abacus.clue.commands;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import abacus.api.query.Facet;
import abacus.api.query.FacetParam;
import abacus.api.query.Request;
import abacus.api.query.Result;
import abacus.api.query.ResultSet;
import abacus.service.AbacusQueryService;

import com.senseidb.clue.commands.ClueCommand;

public class BQLCommand extends ClueCommand {

  private static final String CMD = "bql";
  
  private final AbacusQueryService svc;
  public BQLCommand(AbacusClueContext ctx) {
    super(ctx);
    svc = ctx.getQueryService();
  }
  
  private static Request parse(String bql) {
    // fake a test request for now, TODO: add bql parsing code here
    Request req = new Request();
    FacetParam fp = new FacetParam();
    fp.setMaxNumValues(3);
    Map<String, FacetParam> facetParams = new HashMap<String, FacetParam>();
    facetParams.put("color", fp);
    facetParams.put("tags", fp);
    facetParams.put("category", fp);
    req.setFacetParams(facetParams);
    return req;
  }

  @Override
  public void execute(String[] args, PrintStream out) throws Exception {
    StringBuilder buf = new StringBuilder();
    for (String arg : args) {
      buf.append(arg);
      buf.append(" ");
    }
    String bql = buf.toString().trim();
    out.println("executing bql: " + bql);
    Request req = parse(bql);
    out.println("parsed request: " + req);
    ResultSet results = svc.query(req);
    out.println("total hits: " + results.getNumHits());
    out.println("latency: " + results.getLatencyInMs()+"ms");
    out.println("hits: ");
    for (Result res: results.getResultList()) {
      out.println("\t" + res.getDocid() + "(" + res.getScore() + ")"); 
    }
    out.println("facets: ");
    for (Entry<String,List<Facet>> entry : results.getFacetList().entrySet()) {
      String dim = entry.getKey();
      out.println("\t" + dim);
      for (Facet facet : entry.getValue()) {
        out.println("\t\t" + facet.getValue() + "(" + facet.getCount() + ")");
      }
    }
    out.flush();
  }

  @Override
  public String getName() {
    return CMD;
  }

  @Override
  public String help() {
    return "execute a bql query";
  }

}
