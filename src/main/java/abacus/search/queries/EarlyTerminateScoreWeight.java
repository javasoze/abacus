package abacus.search.queries;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;

public class EarlyTerminateScoreWeight extends Weight {

  private final Query query;
  private final Weight innerWeight;
  private final int numToProcess;
  private final int numToScore;
  private final float defaultScore;
  
  public EarlyTerminateScoreWeight(Query query, Weight innerWeight,
      int numToProcess, int numToScore,
      float defaultScore) {
    this.query = query;
    this.innerWeight = innerWeight;
    this.numToProcess = numToProcess;
    this.numToScore = numToScore;
    this.defaultScore = defaultScore;
  }
  
  @Override
  public Explanation explain(AtomicReaderContext context, int doc)
      throws IOException {
    return innerWeight.explain(context, doc);
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public float getValueForNormalization() throws IOException {
    return innerWeight.getValueForNormalization();
  }

  @Override
  public void normalize(float norm, float topLevelBoost) {
    innerWeight.normalize(norm, topLevelBoost);
  }

  @Override
  public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder,
      boolean topScorer, Bits acceptDocs) throws IOException {
    Scorer innerScorer = innerWeight.scorer(context, scoreDocsInOrder, topScorer, acceptDocs);
    return new EarlyTerminateScoreScorer(this, innerScorer, numToProcess, numToScore, defaultScore);
  }

  
}
