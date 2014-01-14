package abacus.search.queries;

import java.io.IOException;

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

public class EarlyTerminateScoreScorer extends Scorer {

  private final Scorer innerScorer;
  private final int numToProcess;
  private final int numToScore;
  private final float defaultScore;
  private int processedCount;
  
  protected EarlyTerminateScoreScorer(Weight weight, Scorer innerScorer, 
      int numToProcess, int numToScorer, float defaultScore) {
    super(weight);
    this.innerScorer = innerScorer;
    this.numToProcess = numToProcess;
    this.numToScore = numToScorer;
    this.defaultScore = defaultScore;
    this.processedCount = 0;
  }

  @Override
  public float score() throws IOException {
    if (processedCount > numToScore) {
      return defaultScore;
    } else {
      return innerScorer.score();
    }
  }

  @Override
  public int freq() throws IOException {
    return innerScorer.freq();
  }

  @Override
  public int docID() {
    return innerScorer.docID();
  }

  @Override
  public int nextDoc() throws IOException {
    if (processedCount < numToProcess) {
      processedCount++;
      return innerScorer.nextDoc();
    } else {
      return NO_MORE_DOCS;
    }
  }

  @Override
  public int advance(int target) throws IOException {
    if (processedCount < numToProcess) {
      processedCount++;
      return innerScorer.advance(target);
    } else {
      return NO_MORE_DOCS;
    }
  }

  @Override
  public long cost() {
    return Math.min(numToProcess, innerScorer.cost());
  }
}
