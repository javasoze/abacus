package abacus.search.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;

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
  
  public static int[] toIntArray(IntList arr) {
    int[] res = new int[arr.size()];
    for (int i = 0; i < res.length; ++i) {
      res[i] = arr.get(i);
    }
    return res;
  }
  
  public static int[] toIntArray(DocIdSetIterator iter) throws IOException {
    IntList arr = new IntArrayList();
    int doc;
    while ((doc = iter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
      arr.add(doc);
    }
    return toIntArray(arr);
  }
}
