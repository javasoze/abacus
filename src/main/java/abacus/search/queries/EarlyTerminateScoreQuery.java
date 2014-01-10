package abacus.search.queries;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;

public class EarlyTerminateScoreQuery extends Query {

  private final int numToProcess;
  private final int numToScore;
  private final float defaultScore;
  private final Query innerQuery;
  
  public EarlyTerminateScoreQuery(int numToProcess, int numToScore, float defaultScore, Query innerQuery) {
    this.numToProcess = numToProcess;
    this.numToScore = numToScore;
    this.defaultScore = defaultScore;
    this.innerQuery = innerQuery;
  }
  
  @Override
  public Weight createWeight(IndexSearcher searcher) throws IOException {
    Weight innerWeight = innerQuery.createWeight(searcher);
    return new EarlyTerminateScoreWeight(this, innerWeight, numToProcess, numToScore, defaultScore);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    Query rewritten = innerQuery.rewrite(reader);
    if (rewritten == innerQuery) {
      return this;
    } else {
      return new EarlyTerminateScoreQuery(numToProcess, numToScore, defaultScore, rewritten);
    }
  }

  @Override
  public String toString(String field) {
    return innerQuery.toString(field);
  }

}
