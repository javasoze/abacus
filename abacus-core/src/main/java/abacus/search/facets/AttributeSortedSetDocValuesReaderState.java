package abacus.search.facets;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.index.IndexReader;

public class AttributeSortedSetDocValuesReaderState extends
    DefaultSortedSetDocValuesReaderState {

  private SortedMap<String, OrdRange> sortedPrefixOrdRange = new TreeMap<String, OrdRange>();
  public AttributeSortedSetDocValuesReaderState(IndexReader reader, String field)
      throws IOException {
    super(reader, field);
    Map<String, OrdRange> prefixOrdRange = super.getPrefixToOrdRange();
    for (Entry<String, OrdRange> entry : prefixOrdRange.entrySet()) {
      sortedPrefixOrdRange.put(entry.getKey(), entry.getValue());
    }
  }
  
  @Override
  public SortedMap<String, OrdRange> getPrefixToOrdRange() {
    return sortedPrefixOrdRange;
  }  
}
