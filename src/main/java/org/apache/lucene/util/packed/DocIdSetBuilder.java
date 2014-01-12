package org.apache.lucene.util.packed;


public class DocIdSetBuilder {
  public static EliasFanoDocIdSet buildEliasFanoSet(int[] docs) {
    int len = docs == null ? 0 : docs.length;
    int max = docs == null || docs.length == 0 ? 0 : docs[docs.length - 1];
    EliasFanoDocIdSet docset = new EliasFanoDocIdSet(len, max);
    for (int doc : docs) {
      docset.efEncoder.encodeNext(doc);
    }
    return docset;
  }
  
  public static PackedIntsDocIdSet buildPackedInts(final int[] docs, int blockSize) {
    PackedIntsDocIdSet docset = new PackedIntsDocIdSet(blockSize);
    for (int doc : docs) {
      docset.addID(doc);
    }
    return docset;
  }
}
