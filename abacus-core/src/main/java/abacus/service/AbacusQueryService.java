package abacus.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.AbacusAttributeFacetCounts;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;

import abacus.api.query.Facet;
import abacus.api.query.FacetParam;
import abacus.api.query.FacetType;
import abacus.api.query.Request;
import abacus.api.query.Result;
import abacus.api.query.ResultSet;
import abacus.api.query.Selection;
import abacus.api.query.SelectionType;
import abacus.config.FacetIndexedType;
import abacus.config.FacetsConfig;
import abacus.config.IndexDirectoryFacetsConfigReader;
import abacus.search.facets.AttributeSortedSetDocValuesReaderState;
import abacus.search.facets.FacetBucket;
import abacus.search.facets.FacetRangeBuilder;
import abacus.search.facets.FacetRangeBuilder.FacetRange;
import abacus.search.facets.FastDocValuesAtomicReader;
import abacus.search.facets.FastDocValuesAtomicReader.MemType;
import abacus.search.facets.LabelAndOrdFacetCounts;
import abacus.search.facets.NumericBucketFacetCounts;
import abacus.search.facets.NumericFacetCounts;
import abacus.search.facets.SortedDocValuesOrdReader;
import abacus.search.facets.SortedSetDocValuesOrdReader;

public class AbacusQueryService implements Closeable {
  
  private final Map<String, FacetsConfig> configMap;
  private final Map<String, AttributeSortedSetDocValuesReaderState> attrReaderState;
  private final IndexReader reader;
  private final QueryParser queryParser;
  private final DirectoryReader dirReader;
  
  public AbacusQueryService(Directory idxDir, QueryParser queryParser) throws IOException {
	this(idxDir, queryParser, null, MemType.Default);
  }
  
  public AbacusQueryService(Directory idxDir, QueryParser queryParser, Map<String, MemType> loadOptions, MemType defaultMemType) throws IOException {
    dirReader = DirectoryReader.open(idxDir);
    configMap = IndexDirectoryFacetsConfigReader.readerFacetsConfig(dirReader);
    attrReaderState = new HashMap<String, AttributeSortedSetDocValuesReaderState>();
    List<AtomicReaderContext> leaves = dirReader.leaves();
    AtomicReader[] subreaders = new AtomicReader[leaves.size()];
    int i = 0;
    for (AtomicReaderContext leaf : leaves) {
      AtomicReader atomicReader = leaf.reader();
      subreaders[i++] = new FastDocValuesAtomicReader(atomicReader, loadOptions, defaultMemType);
    }
    
    reader = new MultiReader(subreaders, true);
    
    for (Entry<String, FacetsConfig> entry : configMap.entrySet()) {
      String name = entry.getKey();
      FacetsConfig config = entry.getValue();
      if (FacetIndexedType.ATTRIBUTE.equals(config.getFacetType())) {
        AttributeSortedSetDocValuesReaderState readerState = new AttributeSortedSetDocValuesReaderState(reader, name);
        attrReaderState.put(name, readerState);
      }
     }    
    this.queryParser = queryParser;
  }
  
  
  
  private Filter buildFilter(String field, String val, SelectionType type, FacetsConfig config) 
      throws ParseException {
    
    if (SelectionType.RANGE.equals(type)) {
      FacetRange facetRange = FacetRangeBuilder.buildFacetRangeBucket(val, config.getNumericType());
      return facetRange.buildRangeFilter(field);
    } else if (SelectionType.TERM.equals(type)) {
      if (config.getNumericType() != null) {
        switch (config.getNumericType()) {        
        case DOUBLE: {
          double doubleVal = Double.parseDouble(val);
          return NumericRangeFilter.newDoubleRange(field, doubleVal, doubleVal, true, true);
        }
        case FLOAT: {
          float floatVal = Float.parseFloat(val);
          return NumericRangeFilter.newFloatRange(field, floatVal, floatVal, true, true);
        }
        case INT: {
          int intVal = Integer.parseInt(val);
          return NumericRangeFilter.newIntRange(field, intVal, intVal, true, true);
        }
        case LONG: {
          long longVal = Long.parseLong(val);
          return NumericRangeFilter.newLongRange(field, longVal, longVal, true, true);
        }
        default: return null;
        }
      } else {
        return new TermFilter(new Term(field, val));
      }
    } else {
      return null;
    }
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
    
    Filter filter = null;
    if (req.isSetSelections()) {
      Map<String, List<Selection>> selMap = req.getSelections();
      if (!selMap.isEmpty()) {
        BooleanFilter bf = new BooleanFilter();
        for (Entry<String, List<Selection>> entry : selMap.entrySet()) {
          String field = entry.getKey();
          FacetsConfig config = configMap.get(field);
          List<Selection> selList = entry.getValue();
          if (!selList.isEmpty()) {            
            for (Selection sel : selList) {
              Occur occur;              
              switch (sel.getMode()) {
              case MUST:
                occur = Occur.MUST; break;
              case SHOULD:
                occur = Occur.SHOULD; break;
              // TODO case MUST_NOT:
              default:
                occur = Occur.SHOULD;
              }
              int valSize = sel.getValuesSize();
              if (valSize > 0) {
                if (valSize > 1) {
                  BooleanFilter valFilter = new BooleanFilter();
                  for (String selVal : sel.getValues()) {
                    Filter f = buildFilter(field, selVal, sel.getType(), config);
                    if (f != null) {
                      valFilter.add(f, occur);
                    }
                  }
                  bf.add(valFilter, Occur.MUST);
                } else {
                  Filter f = buildFilter(field, sel.getValues().get(0), sel.getType(), config);
                  if (f != null) {
                    bf.add(f, Occur.MUST);
                  }
                }
              }
            }
          }
        }
        if (!bf.clauses().isEmpty()) {
          filter = bf;
        }
      }
    }
    
    FacetsCollector facetsCollector = null;
    if (req.isSetFacetParams() && req.getFacetParams().size() > 0) {
      facetsCollector = new FacetsCollector();
    }
    
    IndexSearcher searcher = new IndexSearcher(reader);
    
    TopScoreDocCollector topDocsCollector = TopScoreDocCollector.create(offset + count, true);
    
    Collector collector = facetsCollector == null ? topDocsCollector :
        MultiCollector.wrap(topDocsCollector, facetsCollector);
    
    searcher.search(query, filter, collector);
    
    TopDocs topDocs = topDocsCollector.topDocs();
    
    ResultSet rs = new ResultSet();
    
    rs.setNumHits(topDocsCollector.getTotalHits());
    rs.setCorpusSize(reader.maxDoc());
    
    List<Result> resList = buildHitResultList(topDocs);
    if (req.isExplain()) {
      for (Result res : resList) {
        Explanation expl = searcher.explain(query, (int) res.getDocid());
        res.setExplanation(String.valueOf(expl));
      }
    }
    rs.setResultList(resList);    
    
    if (facetsCollector != null) {      
      rs.setFacetList(buildFacetResults(configMap, req, facetsCollector));
    }    
    
    rs.setLatencyInMs(System.currentTimeMillis() - start);
    
    return rs;
  }
  
  static List<Result> buildHitResultList(TopDocs topDocs) {
    List<Result> hitResult = new ArrayList<Result>(topDocs.scoreDocs.length);
    for (ScoreDoc sd : topDocs.scoreDocs) {
      Result res = new Result();
      res.setDocid(sd.doc);
      res.setScore(sd.score);
      hitResult.add(res);      
    }
    return hitResult;
  }
  
  private List<Facet> buildFacetList(Entry<String, FacetsConfig> configEntry,
      FacetParam facetParam,
      FacetsCollector collector) throws IOException {
    String field = configEntry.getKey();
    FacetsConfig config = configEntry.getValue();    
    FacetIndexedType type = config.getFacetType();
    Facets facetCounts = null;
    List<FacetBucket> buckets = null;
    if (FacetType.RANGE == facetParam.type) {
      if (facetParam.isSetRanges() && facetParam.getRangesSize() > 0) {         
        buckets = new ArrayList<FacetBucket>(facetParam.getRangesSize());
        for (String range : facetParam.getRanges()) {
          try {
            buckets.add(FacetRangeBuilder.buildFacetRangeBucket(range, config.getNumericType()));
          } catch (ParseException pe) {
            throw new IOException(pe.getMessage(), pe);
          }
        }
      } else {
        String[] defaultRanges = config.getRangeStrings();
        if (defaultRanges != null && defaultRanges.length > 0) {
          buckets = new ArrayList<FacetBucket>(defaultRanges.length);
          for (String range : defaultRanges) {
            try {
              buckets.add(FacetRangeBuilder.buildFacetRangeBucket(range, config.getNumericType()));
            } catch (ParseException pe) {
              throw new IOException(pe.getMessage(), pe);
            }            
          }
        }
      }      
    }
    
    if (FacetIndexedType.NUMERIC == type) {  // numeric
      if (buckets != null) {        
        facetCounts = new NumericBucketFacetCounts(configEntry.getKey(), buckets.toArray(
            new FacetBucket[buckets.size()]), collector); 
      } else {
        facetCounts = new NumericFacetCounts(configEntry.getKey(), collector);
      }
    } else if (FacetIndexedType.SINGLE == type) {
      SortedDocValuesOrdReader ordReader = new SortedDocValuesOrdReader(field);
      facetCounts = new LabelAndOrdFacetCounts(field, ordReader, collector);      
    } else if (FacetIndexedType.MULTI == type) {
      SortedSetDocValuesOrdReader ordReader = new SortedSetDocValuesOrdReader(field);
      facetCounts = new LabelAndOrdFacetCounts(field, ordReader, collector);
    } else if (FacetIndexedType.ATTRIBUTE == type) {
      facetCounts = new AbacusAttributeFacetCounts(attrReaderState.get(field), collector);
    } else {
      throw new IllegalStateException("invalid facet type: " + type);
    }
    
    String path = null;
    if (facetParam.isSetPath()) {
      List<String> pathList = facetParam.getPath();
      if (pathList != null && !pathList.isEmpty()) {
        path = pathList.get(0);
      }
    }
    
    if (facetCounts != null) {      
      FacetResult facetResult = facetCounts.getTopChildren(facetParam.getMaxNumValues(), path, new String[0]);
      List<Facet> facetList = new ArrayList<Facet>(facetResult.labelValues.length);
      for (LabelAndValue labelAndVal : facetResult.labelValues) {
        Facet facet = new Facet();
        facet.setValue(labelAndVal.label);
        facet.setCount(labelAndVal.value.longValue());
        facetList.add(facet);
      }
      return facetList;
    } else {
      return Collections.EMPTY_LIST;
    }
  }
  
  Map<String, List<Facet>> buildFacetResults(Map<String, FacetsConfig> configMap,
      Request req,
      FacetsCollector collector) throws IOException {
    Map<String, FacetParam> facetParams = req.getFacetParams();    
    Map<String, List<Facet>> facetsResult = new HashMap<String, List<Facet>>();
    for (Entry<String, FacetsConfig> entry : configMap.entrySet()) {
      String field = entry.getKey();
      FacetParam fp = facetParams.get(field);
      if (fp != null) {
        facetsResult.put(field, buildFacetList(entry, facetParams.get(field),collector));
      }
    }
    return facetsResult;
  }

  @Override
  public void close() throws IOException {
    reader.close();
    dirReader.close();
  }
}
