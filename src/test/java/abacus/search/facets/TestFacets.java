package abacus.search.facets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.junit.Test;

import abacus.search.facets.BytesRefFacetAccumulator;
import abacus.search.facets.FacetAccumulator;
import abacus.search.facets.FacetBucket;
import abacus.search.facets.FacetValue;
import abacus.search.facets.FastDocValuesAtomicReader;
import abacus.search.facets.MultiBytesRefFacetAccumulator;
import abacus.search.facets.NumericBucketFacetAccumulator;
import abacus.search.facets.NumericFacetAccumulator;

public class TestFacets {
  // test dataset
  private static final List<Document> DOC_LIST = new ArrayList<Document>();
  private static final Directory IDX_DIR = new RAMDirectory();
  
  static {
      Document doc = new Document();
      doc.add(new NumericDocValuesField("id", 1));
      doc.add(new SortedDocValuesField("color", new BytesRef("red")));
      doc.add(new NumericDocValuesField("size", 4));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("rabbit")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("pet")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("animal")));
      DOC_LIST.add(doc);

      doc = new Document();
      doc.add(new NumericDocValuesField("id", 2));
      doc.add(new SortedDocValuesField("color", new BytesRef("red")));
      doc.add(new NumericDocValuesField("size", 2));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("dog")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("pet")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("poodle")));
      DOC_LIST.add(doc);

      doc = new Document();
      doc.add(new NumericDocValuesField("id", 3));
      doc.add(new SortedDocValuesField("color", new BytesRef("green")));
      doc.add(new NumericDocValuesField("size", 4));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("rabbit")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("cartoon")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("funny")));
      DOC_LIST.add(doc);

      doc = new Document();
      doc.add(new NumericDocValuesField("id", 4));
      doc.add(new SortedDocValuesField("color", new BytesRef("blue")));
      doc.add(new NumericDocValuesField("size", 1));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("store")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("pet")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("animal")));
      DOC_LIST.add(doc);
      
      doc = new Document();
      doc.add(new NumericDocValuesField("id", 5));
      doc.add(new SortedDocValuesField("color", new BytesRef("blue"))); 
      doc.add(new NumericDocValuesField("size", 4));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("cartoon")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("funny")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("disney")));  
      DOC_LIST.add(doc);

      doc = new Document();
      doc.add(new NumericDocValuesField("id", 6));
      doc.add(new SortedDocValuesField("color", new BytesRef("green")));
      doc.add(new NumericDocValuesField("size", 6));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("funny")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("humor")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("joke")));  
      DOC_LIST.add(doc);

      doc = new Document();
      doc.add(new NumericDocValuesField("id", 7));
      doc.add(new SortedDocValuesField("color", new BytesRef("red")));
      doc.add(new NumericDocValuesField("size", 2));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("humane")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("dog")));
      doc.add(new SortedSetDocValuesField("tag", new BytesRef("rabbit")));   
      DOC_LIST.add(doc);
      
      IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_44, null);
      try {
        IndexWriter writer = new IndexWriter(IDX_DIR, conf);
        int count = 0;
        for (Document d : DOC_LIST) {
          writer.addDocument(d);          
          // make sure we get multiple segments
          if (count %2 == 1) {
            writer.commit();
          }
          count++;
        }
        writer.commit();
        
        writer.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
  }
  
  void checkFacets(FacetValue[] facetVals, Comparator<FacetValue> comparator) {
    FacetValue prev = null;
    for (FacetValue facetVal : facetVals) {
      if (prev != null) {
        int comp = comparator.compare(prev, facetVal);
        assertTrue(comp <= 0);
      }
      prev = facetVal;
    }
  }
  
  public static IndexReader getIndexReader(Directory dir, boolean useDirect) throws Exception {
    IndexReader reader = DirectoryReader.open(dir);
    if (true) {
      List<AtomicReaderContext> leaves = reader.leaves();
      
      AtomicReader[] subreaders = new AtomicReader[leaves.size()];
      int i = 0;
      for (AtomicReaderContext leaf : leaves) {
        AtomicReader atomicReader = leaf.reader();
        subreaders[i++] = new FastDocValuesAtomicReader(atomicReader, useDirect);
      }
      
      reader = new MultiReader(subreaders, true);
    }
    return reader;
  }
  
  @Test
  public void testMinHit() throws Exception {
    IndexReader reader = getIndexReader(IDX_DIR, false);
    IndexSearcher searcher = new IndexSearcher(reader);
    
    FacetAccumulator sizeFacetCollector = new NumericFacetAccumulator("size");
    BytesRefFacetAccumulator colorFacetCollector = new BytesRefFacetAccumulator("color");
    
    searcher.search(new MatchAllDocsQuery(), MultiCollector.wrap(sizeFacetCollector, colorFacetCollector));
   
    FacetValue[] values = sizeFacetCollector.getTopFacets(10, 3);
    assertEquals(1, values.length);
    assertEquals("4", values[0].getLabel().utf8ToString());
    assertEquals(3, values[0].getCount());
    
    values = colorFacetCollector.getTopFacets(10, 3);
    assertEquals(1, values.length);
    assertEquals("red", values[0].getLabel().utf8ToString());
    assertEquals(3, values[0].getCount());
    reader.close();
  }
  
  @Test
  public void testFacets() throws Exception {
    testFacets(false);
    testFacets(true);
  }
  
  public void testFacets(boolean useDirect) throws Exception {
    IndexReader reader = getIndexReader(IDX_DIR, useDirect);
    IndexSearcher searcher = new IndexSearcher(reader);
    
    TopScoreDocCollector docsCollector = TopScoreDocCollector.create(10, true);
    FacetAccumulator sizeFacetCollector = new NumericFacetAccumulator("size");
    FacetAccumulator colorFacetCollector = new BytesRefFacetAccumulator("color");
    FacetAccumulator tagFacetCollector = new MultiBytesRefFacetAccumulator("tag");
    
    FacetAccumulator sizeRangeFacetCollector = new NumericBucketFacetAccumulator("size", new FacetBucket[] {
        new FacetBucket(new BytesRef("(*, 3]"), 0) {
          @Override
          public void accumulate(long val) {
            if (val <= 3) {
              count++;
            }
          }          
        },
        new FacetBucket(new BytesRef("(3, *)"), 0) {
          @Override
          public void accumulate(long val) {
            if (val > 3) {
              count++;
            }
          }          
        },
        
    });
    
    Collector collector = 
        MultiCollector.wrap(docsCollector, 
            sizeFacetCollector, 
            colorFacetCollector, 
            tagFacetCollector,
            sizeRangeFacetCollector);
    
    searcher.search(new MatchAllDocsQuery(), collector);
    
    TopDocs docs = docsCollector.topDocs();
    assertEquals(reader.numDocs(), docs.totalHits);
    
    FacetValue[] facetValues = sizeFacetCollector.getTopFacets(3);
    checkFacets(facetValues, FacetValue.COUNT_COMPARATOR);    
    
    facetValues = colorFacetCollector.getTopFacets(3);
    checkFacets(facetValues, FacetValue.COUNT_COMPARATOR);    
    
    facetValues = tagFacetCollector.getTopFacets(3);
    checkFacets(facetValues, FacetValue.COUNT_COMPARATOR);
    
    facetValues = sizeRangeFacetCollector.getTopFacets(3);    
    checkFacets(facetValues, FacetValue.COUNT_COMPARATOR);
    
    reader.close();
  }
}
