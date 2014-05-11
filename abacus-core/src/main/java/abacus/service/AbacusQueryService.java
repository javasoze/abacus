package abacus.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;

import abacus.api.query.Facet;
import abacus.api.query.Request;
import abacus.api.query.Result;
import abacus.api.query.ResultSet;
import abacus.config.FacetsConfig;
import abacus.config.IndexDirectoryFacetsConfigReader;
import abacus.search.facets.FastDocValuesAtomicReader;
import abacus.search.facets.FastDocValuesAtomicReader.MemType;

public class AbacusQueryService implements Closeable {
  
  private final Map<String, FacetsConfig> configMap;
  private final IndexReader reader;
  private final QueryParser queryParser;
  
  AbacusQueryService(Directory idxDir, QueryParser queryParser) throws IOException {
    DirectoryReader dirReader = DirectoryReader.open(idxDir);
    configMap = IndexDirectoryFacetsConfigReader.readerFacetsConfig(dirReader);
    List<AtomicReaderContext> leaves = dirReader.leaves();
    AtomicReader[] subreaders = new AtomicReader[leaves.size()];
    int i = 0;
    for (AtomicReaderContext leaf : leaves) {
      AtomicReader atomicReader = leaf.reader();
      subreaders[i++] = new FastDocValuesAtomicReader(atomicReader, configMap, MemType.Default);
    }
    
    reader = new MultiReader(subreaders, true);
    this.queryParser = queryParser;
  }  
  
  public ResultSet query(Request req) throws ParseException, IOException {
    long start = System.currentTimeMillis();
    Query query;
    if (req.isSetQueryString()) {
      query = queryParser.parse(req.getQueryString());
    } else {
      query = new MatchAllDocsQuery();
    }
    int offset, count;
    if (req.isSetPagingParam()) {
      offset = req.getPagingParam().getOffset();
      count = req.getPagingParam().getCount();
    } else {
      offset = 0;
      count = 10;
    }
    
    FacetsCollector facetsCollector = null;
    if (req.isSetFacetParams() && req.getFacetParams().size() > 0) {
      facetsCollector = new FacetsCollector();
    }
    
    IndexSearcher searcher = new IndexSearcher(reader);
    
    TopScoreDocCollector topDocsCollector = TopScoreDocCollector.create(offset + count, true);
    
    Collector collector = facetsCollector == null ? topDocsCollector :
        MultiCollector.wrap(topDocsCollector, facetsCollector);
    
    searcher.search(query, collector);
    
    TopDocs topDocs = topDocsCollector.topDocs();
    
    ResultSet rs = new ResultSet();
    
    rs.setNumHits(topDocsCollector.getTotalHits());
    rs.setResultList(buildHitResultList(topDocs));
    rs.setLatencyInMs(System.currentTimeMillis() - start);
    
    if (facetsCollector != null) {
      rs.setFacetList(buildFacetResults(configMap, facetsCollector));
    }
    
    return rs;
  }
  
  static List<Result> buildHitResultList(TopDocs topDocs) {
    List<Result> hitResult = new ArrayList<Result>(topDocs.scoreDocs.length);
    for (ScoreDoc sd : topDocs.scoreDocs) {
      Result res = new Result();
      res.setDocid(sd.doc);
      res.setScore(sd.score);
    }
    return hitResult;
  }
  
  private static List<Facet> buildFacetList(Entry<String, FacetsConfig> configEntry, 
      FacetsCollector collector) {    
    return null;
  }
  
  static Map<String, List<Facet>> buildFacetResults(Map<String, FacetsConfig> configMap, 
      FacetsCollector collector) {
    Map<String, List<Facet>> facetsResult = new HashMap<String, List<Facet>>();
    for (Entry<String, FacetsConfig> entry : configMap.entrySet()) {
      facetsResult.put(entry.getKey(), buildFacetList(entry, collector));
    }
    return facetsResult;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
