package abacus.search.util;

import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.util.PriorityQueue;

import java.util.Comparator;

public class LabelAndValueUtil {

  // Sort by value first, then by label in reverse lexicographical order
  private static Comparator<LabelAndValue> COMPARATOR = new Comparator<LabelAndValue>() {
    @Override
    public int compare(LabelAndValue o1, LabelAndValue o2) {
      int val = o1.value.intValue() - o2.value.intValue();
      if (val == 0) {
        val = o2.label.compareTo(o1.label);
      }
      return val;
    }
  };

  public static PriorityQueue<LabelAndValue> getPriorityQueue(int size) {
    PriorityQueue<LabelAndValue> pq = new PriorityQueue<LabelAndValue>(size) {
      @Override
      protected boolean lessThan(LabelAndValue v1, LabelAndValue v2) {
        return COMPARATOR.compare(v1, v2) < 0;
      }
    };
    return pq;
  }
}