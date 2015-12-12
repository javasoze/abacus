package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectionTerminatedException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;

public class EarlyTerminationCollector implements LeafCollector, Collector {

  private final LeafCollector inner;
  private final int max;
  private int numCollected;
  public EarlyTerminationCollector(int max, LeafCollector inner) {
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
  public LeafCollector getLeafCollector(LeafReaderContext context)
      throws IOException {
    if (numCollected >= max) {
      throw new CollectionTerminatedException();
    }
    return this;
  }
  @Override
  public boolean needsScores() {
    return false;
  }
}
