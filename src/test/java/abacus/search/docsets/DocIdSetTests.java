package abacus.search.docsets;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.search.DocIdSet;
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
