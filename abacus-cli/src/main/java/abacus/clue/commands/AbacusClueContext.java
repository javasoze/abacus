package abacus.clue.commands;

import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;

import abacus.service.AbacusQueryService;
import abacus.service.QueryParser;

import com.senseidb.clue.ClueConfiguration;
import com.senseidb.clue.ClueContext;

public class AbacusClueContext extends ClueContext {

  private SortedSetDocValuesReaderState facetState;
  private AbacusQueryService svc;
  private QueryParser qparser;
  public AbacusClueContext(Directory dir, ClueConfiguration config, boolean interactiveMode) 
      throws Exception {
    super(dir, config, interactiveMode);
    facetState = new DefaultSortedSetDocValuesReaderState(getIndexReader());
    qparser = new QueryParser() {
      
      @Override
      public Query parse(String rawQuery) throws ParseException {        
        try {
          return AbacusClueContext.super.getQueryBuilder().build(rawQuery);
        } catch (Exception e) {
          throw new ParseException(e.getMessage());
        }
      }      
    };
    
    svc = new AbacusQueryService(getDirectory(), qparser);
  }
  
  public SortedSetDocValuesReaderState getFacetState() {
    return facetState;
  }
  
  public AbacusQueryService getQueryService() {
    return svc;
  }
  
  public QueryParser getQueryParser() {
    return qparser;
  }

}
