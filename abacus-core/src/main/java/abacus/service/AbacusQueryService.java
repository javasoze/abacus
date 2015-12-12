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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;

import abacus.api.AbacusFilter;
import abacus.api.AbacusHit;
import abacus.api.AbacusQuery;
import abacus.api.AbacusRequest;
import abacus.api.AbacusResult;
import abacus.api.AbacusSortField;
import abacus.api.AbacusSortFieldType;
import abacus.api.Facet;
import abacus.api.FacetParam;
import abacus.config.FacetIndexedType;
import abacus.config.FieldConfig;
import abacus.config.IndexDirectoryFacetsConfigReader;
import abacus.indexing.AbacusIndexer;
import abacus.search.facets.AttributeSortedSetDocValuesReaderState;
import abacus.search.facets.FacetBucket;
import abacus.search.facets.FacetRangeBuilder;
import abacus.search.facets.FastDocValuesAtomicReader;
import abacus.search.facets.FastDocValuesAtomicReader.MemType;
import abacus.search.facets.LabelAndOrdFacetCounts;
import abacus.search.facets.NumericBucketFacetCounts;
import abacus.search.facets.NumericFacetCounts;
import abacus.search.facets.SortedDocValuesOrdReader;
import abacus.search.facets.SortedSetDocValuesOrdReader;
import abacus.search.filter.FilterConstructor;
import abacus.search.query.QueryConstructor;

public class AbacusQueryService implements Closeable {

  private final Map<String, FieldConfig> configMap;
  private final Map<String, AttributeSortedSetDocValuesReaderState> attrReaderState;
  private final IndexReader reader;
  private final AbacusQueryParser queryParser;
  private final DirectoryReader dirReader;

  public AbacusQueryService(Directory idxDir, AbacusQueryParser queryParser) throws IOException {
    this(idxDir, queryParser, null, MemType.Default);
  }

  public AbacusQueryService(Directory idxDir, AbacusQueryParser queryParser,
      Map<String, MemType> loadOptions, MemType defaultMemType) throws IOException {
    dirReader = DirectoryReader.open(idxDir);
    configMap = IndexDirectoryFacetsConfigReader.readerFacetsConfig(dirReader);
    attrReaderState = new HashMap<>();
    List<LeafReaderContext> leaves = dirReader.leaves();
    LeafReader[] subReaders = new LeafReader[leaves.size()];
    int i = 0;
    for (LeafReaderContext leaf : leaves) {
      LeafReader atomicReader = leaf.reader();
      subReaders[i++] = new FastDocValuesAtomicReader(atomicReader, loadOptions, defaultMemType);
    }

    reader = new MultiReader(subReaders, true);

    for (Entry<String, FieldConfig> entry : configMap.entrySet()) {
      String name = entry.getKey();
      FieldConfig config = entry.getValue();
      if (FacetIndexedType.ATTRIBUTE.equals(config.getFacetType())) {
        AttributeSortedSetDocValuesReaderState readerState = new AttributeSortedSetDocValuesReaderState(
            reader, name);
        attrReaderState.put(name, readerState);
      }
    }
    this.queryParser = queryParser;
  }

  public AbacusResult query(AbacusRequest req) throws ParseException, IOException {
    long start = System.currentTimeMillis();
    Query query;
    if (req.isSetQuery()) {
      AbacusQuery abacusQuery = req.getQuery();
      query = QueryConstructor.constructQuery(abacusQuery, queryParser);
    } else {
      query = new MatchAllDocsQuery();
    }

    Filter filter = null;
    if (req.isSetFilter()) {
      AbacusFilter abacusFilter = req.getFilter();
      filter = FilterConstructor.constructFilter(abacusFilter, queryParser);
    }

    Sort sort = null;
    if (req.isSetSortFields()) {
      SortField[] sortFields = new SortField[req.getSortFieldsSize()];
      for (int i = 0; i < req.getSortFieldsSize(); ++i) {
        AbacusSortField abacusSortField = req.getSortFields().get(i);
        SortField.Type type = SortField.Type.SCORE;
        if (abacusSortField.getType() == AbacusSortFieldType.STRING) {
          type = SortField.Type.STRING;
        } else if (abacusSortField.getType() == AbacusSortFieldType.INT) {
          type = SortField.Type.INT;
        } else if (abacusSortField.getType() == AbacusSortFieldType.LONG) {
          type = SortField.Type.LONG;
        } else if (abacusSortField.getType() == AbacusSortFieldType.FLOAT) {
          type = SortField.Type.FLOAT;
        } else if (abacusSortField.getType() == AbacusSortFieldType.DOUBLE) {
          type = SortField.Type.DOUBLE;
        }
        sortFields[i] = new SortField(abacusSortField.getField(), type,
            abacusSortField.isReverse());
      }
      sort = new Sort(sortFields);
    }

    FacetsCollector facetsCollector = null;
    if (req.isSetFacetParams() && req.getFacetParams().size() > 0) {
      facetsCollector = new FacetsCollector();
    }

    int offset, count;
    if (req.isSetPagingParam()) {
      offset = req.getPagingParam().getOffset();
      count = req.getPagingParam().getCount();
    } else {
      offset = 0;
      count = 10;
    }

    TopDocsCollector topDocsCollector;
    if (sort == null) {
      topDocsCollector = TopScoreDocCollector.create(offset + count);
    } else {
      topDocsCollector = TopFieldCollector.create(sort, offset + count, false, true, false);
    }
    Collector collector = facetsCollector == null ?
        topDocsCollector :
        MultiCollector.wrap(topDocsCollector, facetsCollector);

    IndexSearcher searcher = new IndexSearcher(reader);
    searcher.search(query, filter, collector);
    System.out.println("search latency: " + (System.currentTimeMillis() - start));

    Map<String, List<Facet>> facetMap = null;
    if (facetsCollector != null) {
      facetMap = buildFacetResults(configMap, req, facetsCollector);
    }

    System.out.println("total latency: " + (System.currentTimeMillis() - start));

    TopDocs topDocs = topDocsCollector.topDocs();

    AbacusResult result = new AbacusResult();

    result.setNumHits(topDocsCollector.getTotalHits());
    result.setTotoalDocs(reader.maxDoc());

    List<AbacusHit> hitList = buildHitResultList(searcher, query, req, topDocs, offset);
    result.setHits(hitList);

    if (facetMap != null) {
      result.setFacetList(facetMap);
    }

    result.setLatencyInMs(System.currentTimeMillis() - start);
    return result;
  }

  static List<AbacusHit> buildHitResultList(IndexSearcher searcher, Query query,
      AbacusRequest request,
      TopDocs topDocs, int offset) throws IOException {
    if (offset >= topDocs.scoreDocs.length) {
      return new ArrayList<>();
    }
    int count = topDocs.scoreDocs.length - offset;
    List<AbacusHit> hitResult = new ArrayList<>(count);
    for (int i = offset; i < topDocs.scoreDocs.length; ++i) {
      ScoreDoc sd = topDocs.scoreDocs[i];
      AbacusHit hit = new AbacusHit();
      hit.setDocid(sd.doc);
      hit.setScore(sd.score);
      if (request.isExplain()) {
        Explanation expl = searcher.explain(query, sd.doc);
        hit.setExplanation(String.valueOf(expl));
      }
      if (request.isFetchSrcData()) {
        String srcData = searcher.doc(sd.doc).get(AbacusIndexer.srcDataFieldName);
        hit.setSrcData(srcData);
      }
      hitResult.add(hit);
    }
    return hitResult;
  }

  private List<Facet> buildFacetList(Entry<String, FieldConfig> configEntry,
      FacetParam facetParam, FacetsCollector collector) throws IOException {
    String field = configEntry.getKey();
    FieldConfig config = configEntry.getValue();
    FacetIndexedType type = config.getFacetType();
    Facets facetCounts;
    List<FacetBucket> buckets = null;
    if (facetParam.isSetRanges() && facetParam.getRangesSize() > 0) {
      buckets = new ArrayList<>(facetParam.getRangesSize());
      for (String range : facetParam.getRanges()) {
        try {
          buckets.add(FacetRangeBuilder.buildFacetRangeBucket(range, config.getFieldType()));
        } catch (ParseException pe) {
          throw new IOException(pe.getMessage(), pe);
        }
      }
    } else {
      String[] defaultRanges = config.getRangeStrings();
      if (defaultRanges != null && defaultRanges.length > 0) {
        buckets = new ArrayList<>(defaultRanges.length);
        for (String range : defaultRanges) {
          try {
            buckets.add(FacetRangeBuilder.buildFacetRangeBucket(range, config.getFieldType()));
          } catch (ParseException pe) {
            throw new IOException(pe.getMessage(), pe);
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

    if (facetCounts != null) {
      FacetResult facetResult = facetCounts
          .getTopChildren(facetParam.getMaxNumValues(), null, new String[0]);
      List<Facet> facetList = new ArrayList<>(facetResult.labelValues.length);
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

  Map<String, List<Facet>> buildFacetResults(Map<String, FieldConfig> configMap,
      AbacusRequest req,
      FacetsCollector collector) throws IOException {
    Map<String, FacetParam> facetParams = req.getFacetParams();
    Map<String, List<Facet>> facetsResult = new HashMap<>();
    for (Entry<String, FieldConfig> entry : configMap.entrySet()) {
      String field = entry.getKey();
      FacetParam fp = facetParams.get(field);
      if (fp != null) {
        facetsResult.put(field, buildFacetList(entry, facetParams.get(field), collector));
      }
    }
    return facetsResult;
  }

  public Map<String, FieldConfig> getConfigMap() {
    return configMap;
  }

  @Override
  public void close() throws IOException {
    reader.close();
    dirReader.close();
  }
}
