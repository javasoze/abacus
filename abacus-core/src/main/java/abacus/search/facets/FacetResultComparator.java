package abacus.search.facets;

import java.util.Comparator;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.LabelAndValue;

public class FacetResultComparator implements Comparator<FacetResult> {
  
  private FacetResultComparator() {}
  
  public static final Comparator<FacetResult> INSTANCE = new FacetResultComparator();
  
  @Override
  public int compare(FacetResult r1, FacetResult r2) {
    LabelAndValue v1 = r1.labelValues[0];
    LabelAndValue v2 = r2.labelValues[0];
    int val = v2.value.intValue() - v1.value.intValue();
    if (val == 0) {
      return v1.label.compareTo(v2.label);
    }
    return val;
  }

}
