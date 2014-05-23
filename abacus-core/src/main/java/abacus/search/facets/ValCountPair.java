package abacus.search.facets;

import org.apache.lucene.util.PriorityQueue;

public class ValCountPair implements Comparable<ValCountPair> {
  long val;
  int count;

  // Sort by count first, then by value in reverse order
  @Override
  public int compareTo(ValCountPair o) {
    int value = count - o.count;
    if (value == 0) {
      value = Long.compare(o.val, val);
    }
    return value;
  }
  
  public static PriorityQueue<ValCountPair> getPriorityQueue(int size) {
    return new PriorityQueue<ValCountPair>(size, false) {

      @Override
      protected boolean lessThan(ValCountPair a, ValCountPair b) {
        return a.compareTo(b) < 0;
      }

    };
  }
}
