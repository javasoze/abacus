package abacus.search.queries;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

public class CountOnlyCollector extends Collector {

  private int count = 0;

  @Override
  public void setScorer(Scorer scorer) throws IOException {
  }

  @Override
  public void collect(int doc) throws IOException {
    count++;
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return false;
  }
  
  public int numCollected() {
    return count;
  }

}
