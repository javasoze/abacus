package abacus.search.facets;

import java.io.File;
import java.util.List;

import org.apache.lucene.facet.params.FacetSearchParams;
import org.apache.lucene.facet.search.CountFacetRequest;
import org.apache.lucene.facet.search.FacetResult;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.IOUtils;

public class SearchSampleLuceneFacetIndex {

  public static void main(String[] args) throws Exception {
    File topDir = new File(args[0]);
    File indexDir = new File(topDir, "index");
    File taxoDir = new File(topDir, "tax");
    DirectoryReader r = DirectoryReader.open(FSDirectory.open(indexDir));
    DirectoryTaxonomyReader taxo = new DirectoryTaxonomyReader(FSDirectory.open(taxoDir));
    
    FacetSearchParams fsp = new FacetSearchParams(
        new CountFacetRequest(new CategoryPath("color"), 10),
        new CountFacetRequest(new CategoryPath("year"), 10),
        new CountFacetRequest(new CategoryPath("price"), 10),
        new CountFacetRequest(new CategoryPath("category"), 10),
        new CountFacetRequest(new CategoryPath("mileage"), 10));
    
    FacetsCollector fc = FacetsCollector.create(fsp, r, taxo);
    TopScoreDocCollector topDocs = TopScoreDocCollector.create(10, false);
    Query csq = new MatchAllDocsQuery();    
    
    IndexSearcher searcher = new IndexSearcher(r);
    
    long start = System.currentTimeMillis();
    searcher.search(csq, MultiCollector.wrap(fc, topDocs));
    long end1 = System.currentTimeMillis();
    List<FacetResult> res = fc.getFacetResults();
    long end = System.currentTimeMillis();
    
    System.out.println("search/collect: " + (end1-start));
    System.out.println("total: " + (end-start));
    System.out.println(res);
    
    IOUtils.close(r, taxo);

  }

}
