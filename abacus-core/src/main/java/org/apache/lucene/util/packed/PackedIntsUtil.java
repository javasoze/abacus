package org.apache.lucene.util.packed;

public class PackedIntsUtil {
  public static int getNumBits(int val) {
    int count = 0;
    while (val > 0) {
      count++;
      val = val >> 1;
    }
    return count;
  }
}
