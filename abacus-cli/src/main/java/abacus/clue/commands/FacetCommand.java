package abacus.clue.commands;

import java.io.PrintStream;

import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;

import com.senseidb.clue.commands.ClueCommand;

public class FacetCommand extends ClueCommand {

  public static final String CMD = "facet";
  
  private SortedSetDocValuesReaderState facetState;
  public FacetCommand(AbacusClueContext ctx) {
    super(ctx);
    facetState = ctx.getFacetState();
  }

  @Override
  public void execute(String[] args, PrintStream out) throws Exception {
    
  }

  @Override
  public String getName() {
    return CMD;
  }

  @Override
  public String help() {
    return "facet command";
  }

}
