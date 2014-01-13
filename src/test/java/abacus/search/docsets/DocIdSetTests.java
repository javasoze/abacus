package abacus.search.docsets;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.lucene.facet.collections.IntArray;
import org.apache.lucene.search.AndNotDocIdSetIterator;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IntArrayDocIdSetIterator;
import org.apache.lucene.search.PairDocIdSetIterator;
import org.apache.lucene.store.InputStreamDataInput;
import org.apache.lucene.store.OutputStreamDataOutput;
import org.apache.lucene.util.packed.DocIdSetBuilder;
import org.apache.lucene.util.packed.PackedIntsDocIdSet;
import org.junit.Test;

import abacus.search.util.DocIdSetIteratorUtil;

public class DocIdSetTests {
	
  @Test
  public void testIntArrayDSI() throws Exception{
    int[] arr = new int[]{-1,-1,1,3,5,7,9};
    IntArrayDocIdSetIterator iter = new IntArrayDocIdSetIterator(arr);
    int doc = iter.nextDoc();
    TestCase.assertEquals(1,doc);
    doc = iter.nextDoc();
    TestCase.assertEquals(3,doc);

    iter.reset();
    for (int i=0;i<5;++i){
      doc = iter.nextDoc();
    }
    TestCase.assertEquals(9,doc);
    doc = iter.nextDoc();
    TestCase.assertEquals(DocIdSetIterator.NO_MORE_DOCS,doc);

    iter.reset();
    doc = iter.advance(6);
    TestCase.assertEquals(7,doc);

    doc = iter.advance(7);
    TestCase.assertEquals(9,doc);

    iter.reset();
    doc = iter.advance(9);
    TestCase.assertEquals(9,doc);
    doc = iter.nextDoc();
    TestCase.assertEquals(DocIdSetIterator.NO_MORE_DOCS,doc);

    iter.reset();
    doc = iter.advance(10);
    TestCase.assertEquals(DocIdSetIterator.NO_MORE_DOCS,doc);


    arr = new int[]{1,3,5,7,9};
    iter = new IntArrayDocIdSetIterator(arr);
    doc = iter.nextDoc();
    doc = iter.nextDoc();
    doc = iter.nextDoc();
    doc = iter.advance(1);
    TestCase.assertEquals(5,doc);
    arr = new int[]{1,3,5,7,9};
    iter = new IntArrayDocIdSetIterator(arr);
    doc = iter.advance(1);
    TestCase.assertEquals(1,doc);
    doc = iter.advance(1);
    TestCase.assertEquals(3,doc);
  }

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
  }
  
  @Test
  public void testPackedIntsDocIdSetSkips() throws Exception {
    int blockSize = 3;
    PackedIntsDocIdSet docidSet;
    DocIdSetIterator iter;
    int[] expected, docs, skips;
    
    docs = new int[] {1, 2, 3, 4};
    docidSet = DocIdSetBuilder.buildPackedInts(docs, blockSize);    
    skips = new int[] {2, 3};
    expected = new int[] {2, 3};
    iter = docidSet.iterator();
    verifySKips(iter, skips, expected);
    
    docs = new int[] {1, 3, 5, 7, 9, 11, 12};
    docidSet = DocIdSetBuilder.buildPackedInts(docs, blockSize);
    skips = new int[] {13, 15, 18};
    expected = new int[] {};
    iter = docidSet.iterator();
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
  
  @Test
  public void testAndNotDocIdSetIterator() throws Exception {

    // test iteration
    int[] d1 = new int[] {1, 2, 3, 4, 5};
    int[] d2 = new int[] {2, 4};
    int[] expected = new int[] {1, 3, 5};
    DocIdSetIterator iter = new AndNotDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    int[] returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));


    d1 = new int[] {1, 2, 3, 4, 5};
    d2 = new int[] {};
    expected = new int[] {1, 2, 3, 4, 5};

    iter = new AndNotDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    d1 = new int[] {1, 2, 3, 4, 5};
    d2 = new int[] {7, 8, 9};
    expected = new int[] {1, 2, 3, 4, 5};

    iter = new AndNotDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    d1 = new int[] {1, 3, 5};
    d2 = new int[] {2, 4};
    expected = new int[] {1, 3, 5};

    iter = new AndNotDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    // test skips
    int[] skipPoints = new int[] {2, 3};
    d1 = new int[] {1, 2, 3, 4, 5};
    d2 = new int[] {2, 4};

    expected = new int[] {3, 5};
    
    iter = new AndNotDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    
    verifySKips(iter, skipPoints, expected);

    skipPoints = new int[] {2, 3};
    d1 = new int[] {1, 2, 3, 4, 5};
    d2 = new int[] {};

    expected = new int[] {2, 3};
    

    iter = new AndNotDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    verifySKips(iter, skipPoints, expected);


    skipPoints = new int[] {1, 4};
    d1 = new int[] {1, 3, 4, 5};
    d2 = new int[] {2, 4};

    expected = new int[] {1, 5};
    
    iter = new AndNotDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    verifySKips(iter, skipPoints, expected);
  }

  @Test
  public void testPairDocIdSetIterator() throws Exception {
    int[] d1 = new int[] {1, 3,  5};
    int[] d2 = new int[] {2, 4, 6};
    int[] expected = new int[] {1, 2, 3, 4, 5, 6};

    DocIdSetIterator iter = new PairDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    int[] returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    d1 = new int[] {1, 3,  5};
    d2 = new int[] {};
    expected = new int[] {1, 3, 5};

    iter = new PairDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    iter = new PairDocIdSetIterator(DocIdSetIterator.empty(), DocIdSetIterator.empty());
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    expected = new int[]{};
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    iter = new PairDocIdSetIterator(new IntArrayDocIdSetIterator(d1), DocIdSetIterator.empty());
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    expected = d1;
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    iter = new PairDocIdSetIterator(DocIdSetIterator.empty(), new IntArrayDocIdSetIterator(d1));
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    expected = d1;
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));


    d1 = new int[] {1, 3,  5};
    d2 = new int[] {1, 4, 5};
    expected = new int[] {1, 3, 4, 5};

    iter = new PairDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    d1 = new int[] {1, 3,  5};
    d2 = new int[] {1, 4, 5, 6};
    expected = new int[] {1, 3, 4, 5, 6};

    iter = new PairDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    d1 = new int[] {1, 3, 5};
    d2 = new int[] {3, 5, 6};
    expected = new int[] {1, 3, 5, 6};

    iter = new PairDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    returned = DocIdSetIteratorUtil.toIntArray(iter);
    assertTrue("expected: " + Arrays.toString(expected) +", got: " + Arrays.toString(returned),
        Arrays.equals(expected, returned));

    d1 = new int[] {1, 3, 5};
    d2 = new int[] {3, 5, 6};
    int[] skipPoints = new int[] {3, 6, 7};
    expected = new int[] {3, 6};
    
    iter = new PairDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    verifySKips(iter, skipPoints, expected);

    d1 = new int[] {1, 3,  5};
    d2 = new int[] {1, 4, 5, 6};
    skipPoints = new int[] {2, 3, 6};
    expected = new int[] {3, 4, 6};
    
    iter = new PairDocIdSetIterator(new IntArrayDocIdSetIterator(d1),
            new IntArrayDocIdSetIterator(d2));
    verifySKips(iter, skipPoints, expected);
  }
}
