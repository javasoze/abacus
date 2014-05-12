package abacus.search.facets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.junit.Test;

import abacus.search.facets.FastDocValuesAtomicReader.MemType;

public class TestFacets {
  
  
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
    IndexReader reader = getIndexReader(FacetTestUtil.IDX_DIR, memType);
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
