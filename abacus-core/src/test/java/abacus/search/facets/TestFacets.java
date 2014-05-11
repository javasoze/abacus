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
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
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

import abacus.search.facets.FastDocValuesAtomicReader.MemType;

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
      
      IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_47, null);
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
  
  void checkFacets(List<FacetResult> facetVals, Comparator<FacetResult> comparator) {
    FacetResult prev = null;
    for (FacetResult facetVal : facetVals) {
      if (prev != null) {
        int comp = comparator.compare(prev, facetVal);
        assertTrue(comp <= 0);
      }
      prev = facetVal;
    }
  }
  
  public static IndexReader getIndexReader(Directory dir, 
      FastDocValuesAtomicReader.MemType memType) throws Exception {
    IndexReader reader = DirectoryReader.open(dir);
    if (true) {
      List<AtomicReaderContext> leaves = reader.leaves();
      
      AtomicReader[] subreaders = new AtomicReader[leaves.size()];
      int i = 0;
      for (AtomicReaderContext leaf : leaves) {
        AtomicReader atomicReader = leaf.reader();
        subreaders[i++] = new FastDocValuesAtomicReader(atomicReader, null, memType);
      }
      
      reader = new MultiReader(subreaders, true);
    }
    return reader;
  }
  
  @Test
  public void testFacets() throws Exception {
    testFacets(MemType.Default);
    testFacets(MemType.Heap);
    testFacets(MemType.Direct);
    testFacets(MemType.Native);
  }
  
  public void testFacets(MemType memType) throws Exception {
    IndexReader reader = getIndexReader(IDX_DIR, memType);
    IndexSearcher searcher = new IndexSearcher(reader);
    
    TopScoreDocCollector docsCollector = TopScoreDocCollector.create(10, true);
    FacetsCollector facetsCollector = new FacetsCollector(false);
    Facets sizeFacet = new NumericFacetCounts("size", facetsCollector);
    Facets colorFacet = new NumericFacetCounts("color", facetsCollector);
    
    Facets sizeRangeFacets = new NumericBucketFacetCounts("size", new FacetBucket[] {
        new FacetBucket("(*, 3]") {
          @Override
          public void accumulate(long val) {
            if (val <= 3) {
              count++;
            }
          }
        },
        new FacetBucket("(3, *)") {
          @Override
          public void accumulate(long val) {
            if (val > 3) {
              count++;
            }
          }
        },
        
    }, facetsCollector);
    
    Collector collector = 
        MultiCollector.wrap(docsCollector, 
            facetsCollector);
    
    searcher.search(new MatchAllDocsQuery(), collector);
    
    TopDocs docs = docsCollector.topDocs();
    assertEquals(reader.numDocs(), docs.totalHits);
    
    List<FacetResult> facetValues = sizeFacet.getAllDims(3);
    checkFacets(facetValues, FacetResultComparator.INSTANCE);    
    
    facetValues = sizeRangeFacets.getAllDims(3);
    checkFacets(facetValues, FacetResultComparator.INSTANCE);
    
    facetValues = colorFacet.getAllDims(3);
    checkFacets(facetValues, FacetResultComparator.INSTANCE);
    
    reader.close();
  }
}
