package abacus.search.facets;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollector.MatchingDocs;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.PriorityQueue;

import abacus.search.util.LabelAndValueUtil;

public class NumericBucketFacetCounts extends Facets {

  private final String field;
  private final SortedMap<String, FacetBucket> bucketMap;

  public NumericBucketFacetCounts(String field, FacetBucket[] buckets,
      FacetsCollector facetCollector)
      throws IOException {
    this.field = field;
    this.bucketMap = new TreeMap<String, FacetBucket>();
    for (FacetBucket bucket : buckets) {
      this.bucketMap.put(bucket.getLabel(), bucket);
    }
    long start = System.currentTimeMillis();
    count(facetCollector.getMatchingDocs());
    System.out.println(field + " counting took: " + (System.currentTimeMillis() - start));
  }

  /** Does all the "real work" of tallying up the counts. */
  private final void count(List<MatchingDocs> matchingDocs) throws IOException {
    FacetBucket[] buckets = bucketMap.values().toArray(new FacetBucket[bucketMap.size()]);
    for (MatchingDocs hits : matchingDocs) {

      AtomicReader reader = hits.context.reader();
      NumericDocValues docValues = reader.getNumericDocValues(field);
      if (docValues == null) {
        continue;
      }

      DocIdSet hitSet = hits.bits;
      if (hitSet != null) {
        DocIdSetIterator hitsIter = hitSet.iterator();
        int docId;
        while ((docId = hitsIter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
          long val = docValues.get(docId);
          for (FacetBucket bucket : buckets) {
            bucket.accumulate(val);
          }
        }
      }
    }
  }

  @Override
  public List<FacetResult> getAllDims(int topN) throws IOException {
    return Collections.singletonList(getTopChildren(topN, field, new String[0]));
  }

  @Override
  public Number getSpecificValue(String dim, String... paths)
      throws IOException {
    if (paths.length != 1) {
      throw new IllegalArgumentException("paths should have length = 1");
    }

    FacetBucket bucket = bucketMap.get(paths[0]);
    return bucket == null ? null : bucket.getCount();
  }

  @Override
  public FacetResult getTopChildren(int topN, String dim, String... paths)
      throws IOException {
    if (paths.length != 0) {
      throw new IllegalArgumentException("paths should have length = 0");
    }

    int sum = 0;
    int childCount = 0;
    PriorityQueue<LabelAndValue> pq = LabelAndValueUtil.getPriorityQueue(topN);
    for (FacetBucket bucket : bucketMap.values()) {
      int count = bucket.getCount();
      if (count > 0) {
        sum += count;
        childCount++;
        pq.insertWithOverflow(new LabelAndValue(bucket.getLabel(), count));
      }
    }

    int numVals = pq.size();
    LabelAndValue[] labelValues = new LabelAndValue[numVals];

    // Priority queue pops out "least" element first (that is the root).
    // Least in our definition regardless of how we define what that is should be the last element.
    for (int i = 0; i < numVals; ++i) {
      labelValues[numVals - i - 1] = pq.pop();
    }
    return new FacetResult(field, new String[0], sum, labelValues, childCount);
  }

}
