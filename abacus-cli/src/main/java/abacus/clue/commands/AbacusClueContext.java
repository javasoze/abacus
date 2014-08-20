package abacus.clue.commands;

import abacus.service.AbacusQueryParser;
import abacus.service.AbacusQueryService;
import com.senseidb.clue.ClueConfiguration;
import com.senseidb.clue.ClueContext;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;

public class AbacusClueContext extends ClueContext {

  private final AbacusQueryService svc;
  public AbacusClueContext(Directory dir, ClueConfiguration config, boolean interactiveMode)
      throws Exception {
    super(dir, config, interactiveMode);
    AbacusQueryParser queryParser = new AbacusQueryParser() {
      @Override
      public Query parse(String rawQuery) throws ParseException {
        try {
          return AbacusClueContext.super.getQueryBuilder().build(rawQuery);
        } catch (Exception e) {
          throw new ParseException(e.getMessage());
        }
      }
    };
    svc = new AbacusQueryService(getDirectory(), queryParser);
  }

  public AbacusQueryService getQueryService() {
    return svc;
  }
}
