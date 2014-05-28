package abacus.search.facets;

import abacus.indexing.AbacusIndexer;
import abacus.search.facets.docvalues.ArrayNumericDocValues;
import abacus.search.facets.docvalues.DirectBufferNumericDocValues;
import abacus.search.facets.docvalues.NativeNumericDocValues;
import junit.framework.TestCase;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

import java.io.File;

/**
 * Created by yozhao on 5/28/14.
 */
public class NumericDocValuesPerf extends TestCase {

  String indexDir = "/tmp/ArrayNumericDocValuesPerf";
  int testScale = 10000000;

  @Override
  public void setUp() throws Exception {
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_48, new StandardAnalyzer(
        Version.LUCENE_48));
    IndexWriter writer = new IndexWriter(
        FSDirectory.open(new File(indexDir)), config);
    for (int i = 0; i < testScale; ++i) {
      Document doc = new Document();
      AbacusIndexer.addNumericField(doc, "id", i);
      writer.addDocument(doc);
    }
    writer.forceMerge(1);
    writer.commit();
    writer.close();
  }

  @Test
  public void testNumericDocValues() throws Exception {
    DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
    AtomicReader atomicReader = directoryReader.leaves().get(0).reader();
    long time0 = System.currentTimeMillis();
    NumericDocValues docValues = atomicReader.getNumericDocValues("id");
    long time1 = System.currentTimeMillis();
    for (int i = 0; i < testScale; ++i) {
      assertEquals(i, docValues.get(i));
    }
    long time2 = System.currentTimeMillis();
    System.out.println(
        "Initialize NumericDocValues took " + (time1 - time0) + "ms");
    System.out.println(
        "Traversing all NumericDocValues took " + (time2 - time1) + "ms");
    directoryReader.close();
  }

  @Test
  public void testArrayNumericDocValues() throws Exception {
    DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
    AtomicReader atomicReader = directoryReader.leaves().get(0).reader();
    long time0 = System.currentTimeMillis();
    NumericDocValues docValues = atomicReader.getNumericDocValues("id");
    ArrayNumericDocValues docValuesWrapper = new ArrayNumericDocValues(docValues,
        atomicReader.maxDoc());
    long time1 = System.currentTimeMillis();
    for (int i = 0; i < testScale; ++i) {
      assertEquals(i, docValuesWrapper.get(i));
    }
    long time2 = System.currentTimeMillis();
    System.out.println(
        "Initialize ArrayNumericDocValues took " + (time1 - time0) + "ms");
    System.out.println(
        "Traversing all ArrayNumericDocValues took " + (time2 - time1) + "ms");
    directoryReader.close();
  }

  @Test
  public void testDirectBufferNumericDocValues() throws Exception {
    DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
    AtomicReader atomicReader = directoryReader.leaves().get(0).reader();
    long time0 = System.currentTimeMillis();
    NumericDocValues docValues = atomicReader.getNumericDocValues("id");
    DirectBufferNumericDocValues docValuesWrapper = new DirectBufferNumericDocValues(docValues,
        atomicReader.maxDoc());
    long time1 = System.currentTimeMillis();
    for (int i = 0; i < testScale; ++i) {
      assertEquals(i, docValuesWrapper.get(i));
    }
    long time2 = System.currentTimeMillis();
    System.out.println(
        "Initialize DirectBufferNumericDocValues took " + (time1 - time0) + "ms");
    System.out.println(
        "Traversing all DirectBufferNumericDocValues took " + (time2 - time1) + "ms");
    directoryReader.close();
  }

  @Test
  public void testNativeNumericDocValues() throws Exception {
    DirectoryReader directoryReader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
    AtomicReader atomicReader = directoryReader.leaves().get(0).reader();
    long time0 = System.currentTimeMillis();
    NumericDocValues docValues = atomicReader.getNumericDocValues("id");
    NativeNumericDocValues docValuesWrapper = new NativeNumericDocValues(docValues,
        atomicReader.maxDoc());
    long time1 = System.currentTimeMillis();
    for (int i = 0; i < testScale; ++i) {
      assertEquals(i, docValuesWrapper.get(i));
    }
    long time2 = System.currentTimeMillis();
    System.out.println(
        "Initialize NativeNumericDocValues took " + (time1 - time0) + "ms");
    System.out.println(
        "Traversing all NativeNumericDocValues took " + (time2 - time1) + "ms");
    docValuesWrapper.close();
    directoryReader.close();
  }

  @Override
  protected void tearDown() throws Exception {
    File file = new File(indexDir);
    FacetTestUtil.deleteDir(file);
  }
}
