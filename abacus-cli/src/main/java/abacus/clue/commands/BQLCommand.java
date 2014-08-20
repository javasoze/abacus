package abacus.clue.commands;

import abacus.api.AbacusFieldType;
import abacus.api.AbacusHit;
import abacus.api.AbacusRequest;
import abacus.api.AbacusResult;
import abacus.api.Facet;
import abacus.api.FacetParam;
import abacus.config.FieldConfig;
import abacus.service.AbacusQueryService;
import bql.parser.BQLCompiler;
import com.senseidb.clue.commands.ClueCommand;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BQLCommand extends ClueCommand {

  private static final String CMD = "bql";
  
  private final AbacusQueryService svc;
  private final BQLCompiler _compiler;
  public BQLCommand(AbacusClueContext ctx) {
    super(ctx);
    svc = ctx.getQueryService();
    Map<String, FieldConfig> configMap = svc.getConfigMap();
    Map<String, AbacusFieldType> fieldTypeMap = new HashMap<>();
    for (Map.Entry<String, FieldConfig> entry : configMap.entrySet()) {
      String field = entry.getKey();
      AbacusFieldType type = entry.getValue().getFieldType();
      fieldTypeMap.put(field, type);
    }
    _compiler = new BQLCompiler(fieldTypeMap);
  }
  
  private AbacusRequest parse(String bql) {
     return _compiler.compile(bql);
  }

  @Override
  public void execute(String[] args, PrintStream out) throws Exception {
    StringBuilder buf = new StringBuilder();
    for (String arg : args) {
      buf.append(arg);
      buf.append(" ");
    }
    String bql = buf.toString().trim();
    if (bql.isEmpty()) {
      out.println("Empty command, usage: bql command");
      return;
    }
    out.println("executing bql: " + bql);
    AbacusRequest req = parse(bql);
    out.println("parsed request: " + req);
    AbacusResult result = svc.query(req);
    out.println("total hits: " + result.getNumHits());
    out.println("latency: " + result.getLatencyInMs()+"ms");
    out.println("hits: ");
    for (AbacusHit hit: result.getHits()) {
      out.println(hit.toString());
    }
    if (result.getFacetList() != null && result.getFacetListSize() > 0) {
      out.println("facets: ");
      for (Map.Entry<String, List<Facet>> entry : result.getFacetList().entrySet()) {
        String dim = entry.getKey();
        out.println("\t" + dim);
        for (Facet facet : entry.getValue()) {
          out.println("\t\t" + facet.getValue() + "(" + facet.getCount() + ")");
        }
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
