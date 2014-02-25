package abacus.clue.commands;

import org.apache.lucene.store.Directory;

import com.senseidb.clue.ClueApplication;
import com.senseidb.clue.ClueConfiguration;
import com.senseidb.clue.ClueContext;

public class AbacusClueApplication extends ClueApplication {

  public AbacusClueApplication(String idxLocation, boolean interactiveMode)
      throws Exception {
    super(idxLocation, interactiveMode);    
  }

  @Override
  public ClueContext newContext(Directory dir, ClueConfiguration config,
      boolean interactiveMode) throws Exception {

    AbacusClueContext ctx =  new AbacusClueContext(dir, config, interactiveMode);
    
    new FacetCommand(ctx);
    return ctx;
  }

}
