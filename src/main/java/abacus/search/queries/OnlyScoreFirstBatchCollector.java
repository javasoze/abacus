package abacus.search.queries;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

public class OnlyScoreFirstBatchCollector extends Collector {

  private final Collector scoreCollector;
  private final Collector otherCollector;
  private final int numToScore;
  private int processedCount = 0;
  
  public OnlyScoreFirstBatchCollector(Collector scoreCollector, 
      Collector otherCollector, int numToScore) {
    this.scoreCollector = scoreCollector;
    this.otherCollector = otherCollector;
    this.numToScore = numToScore;
  }
  @Override
  public void setScorer(Scorer scorer) throws IOException {
    scoreCollector.setScorer(scorer);
    otherCollector.setScorer(scorer);
  }

  @Override
  public void collect(int doc) throws IOException {
    otherCollector.collect(doc);
    if (processedCount < numToScore) {
      scoreCollector.collect(doc);
      processedCount++;
    }
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    scoreCollector.setNextReader(context);
    otherCollector.setNextReader(context);

  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return scoreCollector.acceptsDocsOutOfOrder() && otherCollector.acceptsDocsOutOfOrder();
  }

}
