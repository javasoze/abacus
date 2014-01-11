package org.apache.lucene.util.packed;

import org.apache.lucene.search.DocIdSet;

public class DocIdSetBuilder {
  public static DocIdSet buildEliasFanoSet(int[] docs) {
    EliasFanoDocIdSet docset = new EliasFanoDocIdSet(docs.length, docs[docs.length - 1]);
    for (int doc : docs) {
      docset.efEncoder.encodeNext(doc);
    }
    return docset;
  }
  
  public static DocIdSet buildPackedInts(final int[] docs, int blockSize) {
    PackedIntsDocIdSet docset = new PackedIntsDocIdSet(128);
    for (int doc : docs) {
      docset.addID(doc);
    }
    return docset;
  }
}
