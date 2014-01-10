package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

public class EarlyTerminationCollector extends Collector {

  private final Collector inner;
  private final int max;
  private int numCollected;
  public EarlyTerminationCollector(int max, Collector inner) {
    this.inner = inner;
    this.max = max;
    this.numCollected = 0;
  }
  @Override
  public void setScorer(Scorer scorer) throws IOException {
    inner.setScorer(scorer);
  }

  @Override
  public void collect(int doc) throws IOException {
    if (numCollected >= max) {
      throw new CollectionTerminatedException();
    }
    inner.collect(doc);
    numCollected++;
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    if (numCollected >= max) {
      throw new CollectionTerminatedException();
    }
    inner.setNextReader(context);
  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return inner.acceptsDocsOutOfOrder();
  }

}
