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
    MemType memType = MemType.Heap;
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
    
    for (int i = 0; i < numIter; ++i) {
      long start = System.currentTimeMillis();
      searcher.search(q, 
          //new EarlyTerminationCollector(1000, collector)
          collector
      );
      
      long send = System.currentTimeMillis();
      //TopDocs td = tdCollector.topDocs();
      
      NumericFacetCounts yearFacet = new NumericFacetCounts("year", facetsCollector);
      /*
      LabelAndOrdFacetCounts colorFacetCollector = new BytesRefFacetAccumulator("color");
      
      LabelAndOrdFacetCounts categoryFacetCollector = new BytesRefFacetAccumulator("category");
      
      NumericFacetCounts priceFacetCollector = new NumericFacetCounts("price");
      
      NumericFacetCounts mileageFacetCollector = new NumericFacetCounts("mileage");
      
      LabelAndOrdFacetCounts catchAllFacetCollector = new MultiBytesRefFacetAccumulator("catchall");
        */
     
      FacetResult yearValues = yearFacet.getAllDims(10).get(0);
      /*
      FacetValue[] colorValues = colorFacetCollector.getTopFacets(10, 1);
      FacetValue[] categoryValues = categoryFacetCollector.getTopFacets(10, 1);
      FacetValue[] priceValues = priceFacetCollector.getTopFacets(10, 1);
      FacetValue[] milageValues = mileageFacetCollector.getTopFacets(10, 1);
      //FacetValue[] catchAllValues = catchAllFacetCollector.getTopFacets(10, 1);
       * 
       */
      long end = System.currentTimeMillis();
      
      collectTimes[i] = send - start;
      totalTimes[i] = end - start;
    }
    
    Arrays.sort(collectTimes);
    Arrays.sort(totalTimes);
    
    long sum1, sum2;
    sum1 = sum2 = 0;
    for (int i = 2; i < 9; ++i) {
      sum1 += collectTimes[i];
      sum2 += totalTimes[i];
    }    
    
    System.out.println("search/collect: " + (sum1 / 8));
    System.out.println("took: " + (sum2 / 8));    
    
    //System.out.println("count : " + catchAllValues.length);
    reader.close();
    
  }
}
