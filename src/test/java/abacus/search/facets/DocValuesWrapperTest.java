package abacus.search.facets;

import java.util.Comparator;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DocValuesWrapperTest {

  static final String NUMERIC_FIELD = "numeric";
  static final String SORTED_FIELD = "sorted";
  static final String SORTEDSET_FIELD = "sortedset";
  static final Comparator<BytesRef> BREF_COMPARATOR = BytesRef.getUTF8SortedAsUnicodeComparator();
  
  static RAMDirectory dir = new RAMDirectory();
  static long[] numericVals = new long[] {12, 13, 0, 100};
  static String[] sortedvals = new String[]{"lucene", "facet", "abacus", "search"};
  static String[][] sortedsetvals = new String[][]{
    {"lucene", "search"},
    {"search"},
    {"facet", "abacus", "search"},
    {}};
  
  static IndexReader topReader;
  static AtomicReader atomicReader;
  
  @BeforeClass
  public static void setup() throws Exception {
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, new StandardAnalyzer(Version.LUCENE_47));
    IndexWriter writer = new IndexWriter(dir, config);
    
    for (int i = 0; i < numericVals.length; ++i) {
      Document doc = new Document();
      doc.add(new NumericDocValuesField(NUMERIC_FIELD, numericVals[i]));
      doc.add(new SortedDocValuesField(SORTED_FIELD, new BytesRef(sortedvals[i])));
      String[] sortedSetVals = sortedsetvals[i];
      for (String sortedSetVal : sortedSetVals) {
        doc.add(new SortedSetDocValuesField(SORTEDSET_FIELD, new BytesRef(sortedSetVal)));  
      }
      writer.addDocument(doc);
    }    
    writer.forceMerge(1);
    writer.commit();
    writer.close();
    
    topReader = DirectoryReader.open(dir);
    atomicReader = topReader.leaves().get(0).reader();
  }
  
  @AfterClass
  public static void tearDown() throws Exception {
    topReader.close();
  }
  
  @Test
  public void testArraySortedDocValues() throws Exception {
    SortedDocValues docVals = atomicReader.getSortedDocValues(SORTED_FIELD);
    SortedDocValues arrayWrapperVals = new ArraySortedDocValues(docVals, atomicReader.maxDoc());
    
    TestCase.assertEquals(docVals.getValueCount(), arrayWrapperVals.getValueCount());
    for (int i = 0; i < docVals.getValueCount(); ++i) {
      BytesRef bref1 = new BytesRef();
      docVals.lookupOrd(i, bref1);
      BytesRef bref2 = new BytesRef();
      arrayWrapperVals.lookupOrd(i, bref2);
      TestCase.assertEquals("expected: " + bref1.utf8ToString() + ", got: " + bref2.utf8ToString(),
          0, BREF_COMPARATOR.compare(bref1, bref2)); 
    }
  }
}
