package abacus.clue.commands;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import abacus.api.query.FacetParam;
import abacus.api.query.Request;
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
    Request req = new Request();
    FacetParam fp = new FacetParam();
    fp.setMaxNumValues(10);
    Map<String, FacetParam> facetParams = new HashMap<String, FacetParam>();
    facetParams.put("color", fp);
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
    ResultSet res = svc.query(req);
    out.println(res);
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
