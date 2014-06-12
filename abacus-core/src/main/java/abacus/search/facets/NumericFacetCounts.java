package abacus.search.facets;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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

public class NumericFacetCounts extends Facets {

  private final String field;
  private Long2IntMap countMap = new Long2IntOpenHashMap();

  public NumericFacetCounts(String field, FacetsCollector hits) throws IOException {
    this.field = field;
    countMap.defaultReturnValue(0);
    long start = System.currentTimeMillis();
    count(hits.getMatchingDocs());
    System.out.println(field + " counting took: " + (System.currentTimeMillis() - start));
  }

  /** Does all the "real work" of tallying up the counts. */
  private final void count(List<MatchingDocs> matchingDocs) throws IOException {
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
          int count = countMap.get(val) + 1;
          countMap.put(val, count);
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

    long val = Long.parseLong(paths[0]);

    return countMap.get(val);
  }

  @Override
  public FacetResult getTopChildren(int topN, String dim, String... paths)
      throws IOException {
    if (paths.length != 0) {
      throw new IllegalArgumentException("paths should have length = 0");
    }
    PriorityQueue<ValCountPair> pq = ValCountPair.getPriorityQueue(topN);

    final ObjectIterator<Entry> entryIter = countMap.long2IntEntrySet().iterator();

    int sum = 0;
    int childCount = 0;
    ValCountPair pair = null;
    while (entryIter.hasNext()) {
      Entry entry = entryIter.next();
      int count = entry.getIntValue();
      if (count > 0) {
        if (pair == null) {
          pair = new ValCountPair();
        }
        pair.val = entry.getLongKey();
        pair.count = count;

        sum += count;
        childCount++;
        pair = pq.insertWithOverflow(pair);
      }
    }

    int numVals = pq.size();
    LabelAndValue[] labelValues = new LabelAndValue[numVals];

    ValCountPair node;
    // Priority queue pops out "least" element first (that is the root).
    // Least in our definition regardless of how we define what that is should be the last element.
    for (int i = 0; i < numVals; ++i) {
      node = pq.pop();
      String label = String.valueOf(node.val);
      labelValues[numVals - i - 1] = new LabelAndValue(label, node.count);
    }

    return new FacetResult(field, new String[0], sum, labelValues, childCount);
  }
}
