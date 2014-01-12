package abacus.search.docsets;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.facet.collections.IntArray;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.InputStreamDataInput;
import org.apache.lucene.store.OutputStreamDataOutput;
import org.apache.lucene.util.packed.DocIdSetBuilder;
import org.apache.lucene.util.packed.PackedIntsDocIdSet;
import org.junit.Test;

import abacus.search.util.DocIdSetIteratorUtil;

public class DocIdSetTests {

  @Test
  public void testEliasFanoDocIdSet() throws Exception {
    int[] expected = new int[] {};
    DocIdSet docidSet = DocIdSetBuilder.buildEliasFanoSet(expected);
    int[] returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
    
    expected = new int[] {1, 3, 5};
    docidSet = DocIdSetBuilder.buildEliasFanoSet(expected);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
    
    expected = new int[] {1};
    docidSet = DocIdSetBuilder.buildEliasFanoSet(expected);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
  }
  
  @Test
  public void testPackedIntsDocIdSet() throws Exception {
    int blockSize = 3;
    int[] expected = new int[] {};
    DocIdSet docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    int[] returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
    
    expected = new int[] {1};
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
    
    expected = new int[] {1, 3, 5};
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
    
    expected = new int[] {1, 3, 5, 7, 9, 11, 12};
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
    
    expected = new int[] {1, 2, 3, 4};
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
    
// test skipping
    // TODO: offby one error somewhere, need to fix
   /* int[] docs = new int[] {1, 2, 3, 4};
    docidSet = DocIdSetBuilder.buildPackedInts(docs, blockSize);    
    int[] skips = new int[] {2, 3};
    expected = new int[] {2, 3};
    DocIdSetIterator iter = docidSet.iterator();
    verifySKips(iter, skips, expected);
    */
    int[] docs = new int[] {1, 3, 5, 7, 9, 11, 12};
    docidSet = DocIdSetBuilder.buildPackedInts(docs, blockSize);
    int[] skips = new int[] {13, 15, 18};
    expected = new int[] {};
    DocIdSetIterator iter = docidSet.iterator();
    verifySKips(iter, skips, expected);
    
    docs = new int[] {1, 3, 5, 7, 9, 11, 12};
    docidSet = DocIdSetBuilder.buildPackedInts(docs, blockSize);
    skips = new int[] {4, 9, 13};
    expected = new int[] {5, 9};
    iter = docidSet.iterator();
    verifySKips(iter, skips, expected);
  }
  
  private void verifySKips(DocIdSetIterator iter, int[] skips, int[] expected) throws IOException {
    IntArray arr = new IntArray();
    for (int skip : skips) {
      int doc = iter.advance(skip);
      if (doc != DocIdSetIterator.NO_MORE_DOCS) {
        arr.addToArray(doc);
      }
    }
    int[] docs = DocIdSetIteratorUtil.toIntArray(arr);
    
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(docs),
        Arrays.equals(expected, docs));
  }
  
  private static byte[] toByteArray(PackedIntsDocIdSet docIdSet) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    PackedIntsDocIdSet.serialize(docIdSet, new OutputStreamDataOutput(bout));
    bout.flush();
    return bout.toByteArray(); 
  }
  
  private static PackedIntsDocIdSet fromByteArray(byte[] bytes) throws IOException {
    ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
    return PackedIntsDocIdSet.deserialize(new InputStreamDataInput(bin));
  }
  
  @Test
  public void testPackedIntsSerialization() throws Exception {
    int blockSize = 3;
    int[] expected = new int[] {1};
    PackedIntsDocIdSet docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    
    byte[] bytes = toByteArray(docidSet);
    docidSet = fromByteArray(bytes);
    int[] returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));    
    
    expected = new int[] {1, 3, 5};
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    bytes = toByteArray(docidSet);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));
    
    expected = new int[] {1, 3, 5, 7, 9, 11, 12};
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    bytes = toByteArray(docidSet);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned)); 
    
    expected = new int[] {};
    docidSet = DocIdSetBuilder.buildPackedInts(expected, blockSize);
    bytes = toByteArray(docidSet);
    docidSet = fromByteArray(bytes);
    returned = DocIdSetIteratorUtil.toIntArray(docidSet.iterator());
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));    
    
  }
}
