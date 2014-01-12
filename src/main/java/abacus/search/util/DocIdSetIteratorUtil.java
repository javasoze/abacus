package abacus.search.util;

import java.io.IOException;

import org.apache.lucene.facet.collections.IntArray;
import org.apache.lucene.search.DocIdSetIterator;

public class DocIdSetIteratorUtil {

  private DocIdSetIteratorUtil() {}
  
  public static String toString(DocIdSetIterator iter) throws IOException {
    StringBuilder buf = new StringBuilder();
    buf.append("[");
    int doc;
    boolean firstPass = true;
    while ((doc = iter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
      if (firstPass) {
        firstPass = false;
      } else {
        buf.append(", ");
      }
      buf.append(doc);
    }
    buf.append("]");
    return buf.toString();
  }
  
  public static int[] toIntArray(IntArray arr) {
    int[] res = new int[arr.size()];
    for (int i = 0; i < res.length; ++i) {
      res[i] = arr.get(i);
    }
    return res;
  }
  
  public static int[] toIntArray(DocIdSetIterator iter) throws IOException {
    IntArray arr = new IntArray();
    int doc;
    while ((doc = iter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
      arr.addToArray(doc);
    }
    return toIntArray(arr);
  }
}
