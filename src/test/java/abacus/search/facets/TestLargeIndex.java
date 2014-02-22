package abacus.search.facets;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import abacus.search.facets.FastDocValuesAtomicReader.MemType;

public class TestLargeIndex {
  
  public static IndexReader getIndexReader(Directory dir, MemType memType) throws Exception {
    IndexReader reader = DirectoryReader.open(dir);
    List<AtomicReaderContext> leaves = reader.leaves();
    
    AtomicReader[] subreaders = new AtomicReader[leaves.size()];
    int i = 0;
    for (AtomicReaderContext leaf : leaves) {
      AtomicReader atomicReader = leaf.reader();
      subreaders[i++] = new FastDocValuesAtomicReader(atomicReader, memType);
    }
    
    reader = new MultiReader(subreaders, true);
    
    return reader;
  }
  
  public static void main(String[] args) throws Exception {
    File idxDir = new File(args[0]);
    MemType memType = MemType.Native;
    Directory dir = FSDirectory.open(idxDir);
    IndexReader reader = getIndexReader(dir, memType);
    
    IndexSearcher searcher = new IndexSearcher(reader);
    
    FacetsCollector facetsCollector = new FacetsCollector(false);
    
    Query q = new MatchAllDocsQuery();
    
    int numIter = 10;
    long[] collectTimes = new long[numIter];
    long[] totalTimes = new long[numIter];
    searcher.search(q, facetsCollector);
    for (int i = 0; i < numIter; ++i) {      
      long start = System.currentTimeMillis();
      //TopDocs td = tdCollector.topDocs();
      
      NumericFacetCounts yearFacet = new NumericFacetCounts("year", facetsCollector);
      
      NumericFacetCounts mileageFacet = new NumericFacetCounts("mileage", facetsCollector);
      
      LabelAndOrdFacetCounts colorFacet = new LabelAndOrdFacetCounts("color", new SortedDocValuesOrdReader("color"), facetsCollector);
      
      LabelAndOrdFacetCounts categoryFacet = new LabelAndOrdFacetCounts("category", new SortedDocValuesOrdReader("category"), facetsCollector);
      
      NumericBucketFacetCounts priceFacet = new NumericBucketFacetCounts("price", new FacetBucket[] {
          new FacetBucket("cheap") {
            @Override
            public final void accumulate(long val) {
              if (val <= 5000) {
                count ++;
              }
            }            
          },
          new FacetBucket("expensive") {
            @Override
            public final void accumulate(long val) {
              if (val > 5000) {
                count ++;
              }
            }
            
          }
      }, facetsCollector);
      
      LabelAndOrdFacetCounts catchAllFacet = new LabelAndOrdFacetCounts("catchall", new SortedSetDocValuesOrdReader("catchall"), facetsCollector);
     
      FacetResult yearValues = yearFacet.getAllDims(10).get(0);
      FacetResult colorValues = colorFacet.getAllDims(10).get(0);
      FacetResult categoryValues = categoryFacet.getAllDims(10).get(0);
      FacetResult priceValues = priceFacet.getAllDims(10).get(0);
      FacetResult mileageValues = mileageFacet.getAllDims(10).get(0);
      FacetResult catchAllValues = catchAllFacet.getAllDims(10).get(0);
      
      if (true) {
        System.out.println(yearValues);
        System.out.println(colorValues);
        System.out.println(categoryValues);
        System.out.println(priceValues);
        System.out.println(mileageValues);
        System.out.println(catchAllValues);
      }
      
      totalTimes[i] = System.currentTimeMillis() - start;      
    }
    
    Arrays.sort(collectTimes);
    Arrays.sort(totalTimes);
    
    long sum = 0;
    for (int i = 2; i < 9; ++i) {      
      sum += totalTimes[i];
    }    
    
    System.out.println("took: " + (sum / 8));    
    
    reader.close();
    
  }
}
