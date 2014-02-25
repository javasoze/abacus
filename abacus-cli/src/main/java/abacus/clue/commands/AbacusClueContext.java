package abacus.clue.commands;

import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.store.Directory;

import com.senseidb.clue.ClueConfiguration;
import com.senseidb.clue.ClueContext;

public class AbacusClueContext extends ClueContext {

  private SortedSetDocValuesReaderState facetState;
  public AbacusClueContext(Directory dir, ClueConfiguration config, boolean interactiveMode) 
      throws Exception {
    super(dir, config, interactiveMode);
    facetState = new DefaultSortedSetDocValuesReaderState(getIndexReader());
  }
  
  public SortedSetDocValuesReaderState getFacetState() {
    return facetState;
  }

}
