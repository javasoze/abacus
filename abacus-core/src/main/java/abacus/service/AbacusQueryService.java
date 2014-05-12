package abacus.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
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
import abacus.api.query.FacetParam;
import abacus.api.query.Request;
import abacus.api.query.Result;
import abacus.api.query.ResultSet;
import abacus.config.FacetType;
import abacus.config.FacetsConfig;
import abacus.config.IndexDirectoryFacetsConfigReader;
import abacus.search.facets.FastDocValuesAtomicReader;
import abacus.search.facets.FastDocValuesAtomicReader.MemType;
import abacus.search.facets.LabelAndOrdFacetCounts;
import abacus.search.facets.NumericFacetCounts;
import abacus.search.facets.SortedDocValuesOrdReader;
import abacus.search.facets.SortedSetDocValuesOrdReader;

public class AbacusQueryService implements Closeable {
  
  private final Map<String, FacetsConfig> configMap;
  private final Map<String, SortedSetDocValuesReaderState> attrReaderState;
  private final IndexReader reader;
  private final QueryParser queryParser;
  
  public AbacusQueryService(Directory idxDir, QueryParser queryParser) throws IOException {
    DirectoryReader dirReader = DirectoryReader.open(idxDir);
    configMap = IndexDirectoryFacetsConfigReader.readerFacetsConfig(dirReader);
    attrReaderState = new HashMap<String, SortedSetDocValuesReaderState>();
    List<AtomicReaderContext> leaves = dirReader.leaves();
    AtomicReader[] subreaders = new AtomicReader[leaves.size()];
    int i = 0;
    for (AtomicReaderContext leaf : leaves) {
      AtomicReader atomicReader = leaf.reader();
      subreaders[i++] = new FastDocValuesAtomicReader(atomicReader, configMap, MemType.Default);
    }
    
    reader = new MultiReader(subreaders, true);
    
    for (Entry<String, FacetsConfig> entry : configMap.entrySet()) {
      String name = entry.getKey();
      FacetsConfig config = entry.getValue();
      if (FacetType.ATTRIBUTE.equals(config.getFacetType())) {
        SortedSetDocValuesReaderState readerState = new DefaultSortedSetDocValuesReaderState(reader);
        attrReaderState.put(name, readerState);
      }
     }
    
    
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
      rs.setFacetList(buildFacetResults(configMap, req.getFacetParams(), facetsCollector));
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
  
  private List<Facet> buildFacetList(Entry<String, FacetsConfig> configEntry, 
      FacetParam facetParam,
      FacetsCollector collector) throws IOException {
    String field = configEntry.getKey();
    FacetsConfig config = configEntry.getValue();
    FacetType type = config.getFacetType();
    Facets facetCounts;
    if (FacetType.NUMERIC == type) {  // numeric
      facetCounts = new NumericFacetCounts(configEntry.getKey(), collector);      
    } else if (FacetType.SINGLE == type) {
      SortedDocValuesOrdReader ordReader = new SortedDocValuesOrdReader(field);
      facetCounts = new LabelAndOrdFacetCounts(field, ordReader, collector);      
    } else if (FacetType.MULTI == type) {
      SortedSetDocValuesOrdReader ordReader = new SortedSetDocValuesOrdReader(field);
      facetCounts = new LabelAndOrdFacetCounts(field, ordReader, collector);
    } else if (FacetType.ATTRIBUTE == type) {
      facetCounts = new SortedSetDocValuesFacetCounts(attrReaderState.get(field), collector);
    } else {
      throw new IllegalStateException("invalid facet type: " + type);
    }
    List<FacetResult> facetResults = facetCounts.getAllDims(facetParam.maxNumValues);
    return null;
  }
  
  Map<String, List<Facet>> buildFacetResults(Map<String, FacetsConfig> configMap,
      Map<String, FacetParam> facetParams,
      FacetsCollector collector) throws IOException {
    Map<String, List<Facet>> facetsResult = new HashMap<String, List<Facet>>();
    for (Entry<String, FacetsConfig> entry : configMap.entrySet()) {
      String field = entry.getKey();
      facetsResult.put(field, buildFacetList(entry, facetParams.get(field),collector));
    }
    return facetsResult;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
