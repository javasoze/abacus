package org.apache.lucene.util.packed;


public class DocIdSetBuilder {  
  public static PackedIntsDocIdSet buildPackedInts(final int[] docs, int blockSize) {
    PackedIntsDocIdSet docset = new PackedIntsDocIdSet(blockSize);
    for (int doc : docs) {
      docset.addID(doc);
    }
    return docset;
  }
}
