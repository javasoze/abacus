package org.apache.lucene.facet.sortedset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState.OrdRange;

import abacus.search.facets.AttributeSortedSetDocValuesReaderState;

public class AbacusAttributeFacetCounts extends SortedSetDocValuesFacetCounts {

  private final AttributeSortedSetDocValuesReaderState state;
  public AbacusAttributeFacetCounts(AttributeSortedSetDocValuesReaderState state,
      FacetsCollector hits) throws IOException {
    super(state, hits);
    this.state = state;
  }

  @Override
  public FacetResult getTopChildren(int topN, String dim, String... path)
      throws IOException {
    // no dim specified, get the attribute names
    if (dim == null) {      
      SortedMap<String, OrdRange> prefixOrdRange = state.getPrefixToOrdRange();
      List<LabelAndValue> labelValList = new ArrayList<LabelAndValue>(topN);
      for (Entry<String, OrdRange> entry : prefixOrdRange.entrySet()) {
        
        OrdRange ordRange = entry.getValue();
        long totalCounts = 0L;
        for (int i = ordRange.start; i <= ordRange.end; ++i) {
          totalCounts += super.counts[i];
        }        
        labelValList.add(new LabelAndValue(entry.getKey(), totalCounts));
        if (labelValList.size() >= topN) {
          break;
        }
      }
      return new FacetResult(null, new String[0], 0, 
          labelValList.toArray(new LabelAndValue[labelValList.size()]) ,0);      
    } else {
      return super.getTopChildren(topN, dim, path);
    }    
  }
  
  
}
