package abacus.search.facets;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsCollector.MatchingDocs;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;
import org.apache.lucene.util.PriorityQueue;

public class LabelAndOrdFacetCounts extends Facets {

  private final String field;
  private final FacetOrdReader ordReader;
  private final List<PerSegmentFacetCount> segmentCountList;
  
  public LabelAndOrdFacetCounts(String field, FacetOrdReader ordReader, FacetsCollector facetCollector) 
      throws IOException {
    this.field = field;
    this.ordReader = ordReader;
    List<MatchingDocs> matchingDocs = facetCollector.getMatchingDocs();
    segmentCountList = new ArrayList<PerSegmentFacetCount>(matchingDocs.size());
    count(matchingDocs);
  }
  
  protected int[] newCountArray(int numVals) {
    return new int[numVals];
  }
  
  /** Does all the "real work" of tallying up the counts. */
  private final void count(List<MatchingDocs> matchingDocs) throws IOException {
    for(MatchingDocs hits : matchingDocs) {
      final FacetOrdSegmentReader ordSegmentReader = ordReader.getSegmentOrdReader(ordReader.getIndexedFieldName(), hits.context);
      
      if (ordSegmentReader == null) {
        continue;
      }
      int[] counts = newCountArray(ordSegmentReader.getValueCount());
      
      PerSegmentFacetCount segmentCount = new PerSegmentFacetCount(counts) {
        
        @Override
        public void lookupLabel(int ord, BytesRef result) {
          ordSegmentReader.lookupLabel(ord, result);
        }

        @Override
        public int getOrd(BytesRef label) {
          return ordSegmentReader.lookupOrd(label);
        }
      };
      
      segmentCountList.add(segmentCount);
      
      DocIdSet hitSet = hits.bits;
      if (hitSet != null) {
        DocIdSetIterator hitsIter = hitSet.iterator();
        int docid;
        while ((docid = hitsIter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
          IntIterator ordIter = ordSegmentReader.getOrds(docid);
          if (ordIter instanceof SingleValueIntIterator) {
            segmentCount.accumulate(ordIter.nextInt());
          } else {
            while (ordIter.hasNext()) {
              segmentCount.accumulate(ordIter.nextInt());
            }
          }
        }
      }      
    }
  }

  @Override
  public FacetResult getTopChildren(int topN, String dim, String... path)
      throws IOException {
    // only 1 segment
    if (segmentCountList.size() == 0) {
      // empty result
      return new FacetResult(field, new String[0], 0, new LabelAndValue[0], 0);
    } else {
      PriorityQueue<ValCountPair> pq = ValCountPair.getPriorityQueue(topN);
      FacetEntryIterator facetIterator = null;
      if (segmentCountList.size() == 1) {
        facetIterator = segmentCountList.get(0).getFacetEntryIterator();
      } else {
        final BytesRefHash labelHash = new BytesRefHash();
        final IntList countList = new IntArrayList();
        for (PerSegmentFacetCount segmentCount : segmentCountList) {          
          FacetEntryIterator perSegIterator = segmentCount.getFacetEntryIterator();
          ValCountPair pair = new ValCountPair();
          BytesRef label = new BytesRef();
          while (perSegIterator.next(pair)) {
            perSegIterator.lookupLabel(pair.val, label);            
            int id;
            if ((id = labelHash.find(label)) < 0) {
              // not found
              labelHash.add(label);
              countList.add(pair.count);
            } else {
              int count = countList.get(id) + pair.count;
              countList.set(id, count);
            }
          }
        }
        
        
        facetIterator = new FacetEntryIterator() {
          int idx = -1;
          @Override
          public boolean next(ValCountPair val) {
            idx++;
            if (idx < countList.size()) {
              val.count = countList.getInt(idx);
              val.val = idx;
              return true; 
            }
            return false;
          }
          
          @Override
          public void lookupLabel(long val, BytesRef label) {
            labelHash.get((int) val, label);            
          }
        };        
      }
      
      ValCountPair pair = new ValCountPair();
      int sum = 0;
      int childCount = 0;
      while (facetIterator.next(pair)) {
        if (pair.count > 0) {
          sum += pair.count;
          childCount ++;
          pair = pq.insertWithOverflow(pair);
          if (pair == null) {
            pair = new ValCountPair();
          }
        }
      }
      
      int numVals = pq.size();
      LabelAndValue[] labelValues = new LabelAndValue[numVals];
      for (int i = 0; i < numVals; ++i) {    
        pair = pq.pop();
        BytesRef labelRef = new BytesRef();
        facetIterator.lookupLabel(pair.val, labelRef);
        labelValues[numVals - i - 1] = new LabelAndValue(labelRef.utf8ToString(), pair.count);
      }
      
      return new FacetResult(dim, new String[0], sum , labelValues, childCount);
    }
  }

  @Override
  public Number getSpecificValue(String dim, String... path) throws IOException {
    if (path.length != 1) {
      throw new IllegalArgumentException("path must of length 1");
    }
    String label = path[0];
    int sum = 0;
    
    for (PerSegmentFacetCount segmentCount : segmentCountList) {
      sum += segmentCount.getCountForLabel(label);
    }
    return sum;
  }

  @Override
  public List<FacetResult> getAllDims(int topN) throws IOException {
    return Collections.singletonList(getTopChildren(topN, field, new String[0]));
  }

}
