package abacus.clue.commands;

import abacus.api.AbacusRequest;
import abacus.service.AbacusQueryService;
import com.senseidb.clue.commands.ClueCommand;

import java.io.PrintStream;

public class BQLCommand extends ClueCommand {

  private static final String CMD = "bql";
  
  private final AbacusQueryService svc;
  public BQLCommand(AbacusClueContext ctx) {
    super(ctx);
    svc = ctx.getQueryService();
  }
  
  private static AbacusRequest parse(String bql) {
    /*
    // fake a test request for now, TODO: add bql parsing code here
    AbacusRequest req = new AbacusRequest();
    FacetParam fp = new FacetParam();
    fp.setMaxNumValues(3);
    Map<String, FacetParam> facetParams = new HashMap<String, FacetParam>();
    facetParams.put("color", fp);
    facetParams.put("tags", fp);
    
    
    FacetParam rangeFacet = new FacetParam();
    rangeFacet.setType(FacetType.RANGE);
    rangeFacet.setMaxNumValues(10);
    facetParams.put("price", rangeFacet);
    
    FacetParam attrParam = new FacetParam();
    attrParam.setType(FacetType.PATH);
    attrParam.addToPath("color");
    facetParams.put("attribute", attrParam);    
    
    req.setFacetParams(facetParams);
    */
    return null;
  }

  @Override
  public void execute(String[] args, PrintStream out) throws Exception {
    /*
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
    */
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
