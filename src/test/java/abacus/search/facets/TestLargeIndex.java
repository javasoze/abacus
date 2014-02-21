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
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopScoreDocCollector;
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
    
    TopScoreDocCollector tdCollector = TopScoreDocCollector.create(20, true);
    FacetsCollector facetsCollector = new FacetsCollector(false);
    
    //QueryParser qp = new QueryParser(Version.LUCENE_44, "contents", new StandardAnalyzer(Version.LUCENE_44));
    //Query q = qp.parse("tags_indexed:macho");
    Query q = new MatchAllDocsQuery();
    
    Collector collector = MultiCollector.wrap(
        //tdCollector,
        facetsCollector
    );
    
    int numIter = 10;
    long[] collectTimes = new long[numIter];
    long[] totalTimes = new long[numIter];
    searcher.search(q, 
        //new EarlyTerminationCollector(1000, collector)
        collector
    );
    for (int i = 0; i < numIter; ++i) {      
      long start = System.currentTimeMillis();
      //TopDocs td = tdCollector.topDocs();
      
      NumericFacetCounts yearFacet = new NumericFacetCounts("year", facetsCollector);
      
      LabelAndOrdFacetCounts colorFacet = new LabelAndOrdFacetCounts("color", new SortedDocValuesOrdReader("color"), facetsCollector);
      
      LabelAndOrdFacetCounts categoryFacet = new LabelAndOrdFacetCounts("category", new SortedDocValuesOrdReader("category"), facetsCollector);
      /*
      NumericFacetCounts priceFacetCollector = new NumericFacetCounts("price");
      
      NumericFacetCounts mileageFacetCollector = new NumericFacetCounts("mileage");
      */
      LabelAndOrdFacetCounts catchAllFacet = new LabelAndOrdFacetCounts("catchall", new SortedSetDocValuesOrdReader("catchall"), facetsCollector);
     
      FacetResult yearValues = yearFacet.getAllDims(10).get(0);
      FacetResult colorValues = colorFacet.getAllDims(10).get(0);
      FacetResult categoryValues = categoryFacet.getAllDims(10).get(0);
      FacetResult catchAllValues = catchAllFacet.getAllDims(10).get(0);
      
      if (false) {
        System.out.println(yearValues);
        System.out.println(colorValues);
        System.out.println(categoryValues);
        System.out.println(catchAllValues);
      }
      //FacetResult catchAllValues = catchAllFacetCollector.getAllDims(10).get(0);
      /*
      FacetValue[] priceValues = priceFacetCollector.getTopFacets(10, 1);
      FacetValue[] milageValues = mileageFacetCollector.getTopFacets(10, 1);
       * 
       */
      totalTimes[i] = System.currentTimeMillis() - start;      
    }
    
    Arrays.sort(collectTimes);
    Arrays.sort(totalTimes);
    
    long sum = 0;
    for (int i = 2; i < 9; ++i) {      
      sum += totalTimes[i];
    }    
    
    System.out.println("took: " + (sum / 8));    
    
    //System.out.println("count : " + catchAllValues.length);
    reader.close();
    
  }
}
