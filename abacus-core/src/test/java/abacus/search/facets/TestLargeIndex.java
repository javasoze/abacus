package abacus.search.facets;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
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
      subreaders[i++] = new FastDocValuesAtomicReader(atomicReader, null, memType);
    }
    
    reader = new MultiReader(subreaders, true);
    
    return reader;
  }
  
  static void doAccumulateSeparateFields(FacetsCollector facetsCollector) throws IOException {
    
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
    
    LabelAndOrdFacetCounts tagsFacet = new LabelAndOrdFacetCounts("tags", new SortedSetDocValuesOrdReader("tags"), facetsCollector);
   
    FacetResult yearValues = yearFacet.getAllDims(10).get(0);
    FacetResult colorValues = colorFacet.getAllDims(10).get(0);
    FacetResult categoryValues = categoryFacet.getAllDims(10).get(0);
    FacetResult priceValues = priceFacet.getAllDims(10).get(0);
    FacetResult mileageValues = mileageFacet.getAllDims(10).get(0);
    FacetResult tagsValues = tagsFacet.getAllDims(10).get(0);
    
    if (false) {
      System.out.println(yearValues);
      System.out.println(colorValues);
      System.out.println(categoryValues);
      System.out.println(priceValues);
      System.out.println(mileageValues);
      System.out.println(tagsValues);
    }
  }
  
  static void doAccumulateFacetsField(SortedSetDocValuesReaderState state, FacetsCollector facetsCollector) throws IOException {
    
    
    SortedSetDocValuesFacetCounts facetCounts = new SortedSetDocValuesFacetCounts(state, facetsCollector);
    
    
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
    
   
    FacetResult yearValues = facetCounts.getTopChildren(10, "year",  new String[0]);
    FacetResult colorValues = facetCounts.getTopChildren(10, "color",  new String[0]);
    FacetResult categoryValues = facetCounts.getTopChildren(10, "category",  new String[0]);
    FacetResult tagsValues = facetCounts.getTopChildren(10, "tags",  new String[0]);
    FacetResult priceValues = priceFacet.getAllDims(10).get(0);
    FacetResult mileageValues = facetCounts.getTopChildren(10, "mileage",  new String[0]);
    
    if (false) {
      System.out.println(yearValues);      
      System.out.println(colorValues);
      System.out.println(categoryValues);
      System.out.println(priceValues);
      System.out.println(mileageValues);
      System.out.println(tagsValues);
    }
  }
  
  public static void main(String[] args) throws Exception {
    File idxDir = new File(args[0]);
    MemType memType = MemType.Native;
    Directory dir = FSDirectory.open(idxDir);
    IndexReader reader = getIndexReader(dir, memType);
    
    SortedSetDocValuesReaderState state = new DefaultSortedSetDocValuesReaderState(reader);
    
    IndexSearcher searcher = new IndexSearcher(reader);
    
    FacetsCollector facetsCollector = new FacetsCollector(false);
    
    //QueryParser qp = new QueryParser(Version.LUCENE_47, "contents", new StandardAnalyzer(Version.LUCENE_47));
    //Query q = qp.parse("tags_indexed:macho");
    Query q = new MatchAllDocsQuery();
    
    int numIter = 10;
    long[] collectTimes = new long[numIter];
    long[] totalTimes = new long[numIter];
    searcher.search(q, facetsCollector);
    for (int i = 0; i < numIter; ++i) {      
      long start = System.currentTimeMillis();
      
      doAccumulateSeparateFields(facetsCollector);
      //doAccumulateFacetsField(state, facetsCollector);
      
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
