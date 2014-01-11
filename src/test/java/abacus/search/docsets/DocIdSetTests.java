package abacus.search.docsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.util.packed.DocIdSetBuilder;
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
}
