package abacus.search.facets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.junit.Test;

import abacus.search.facets.FastDocValuesAtomicReader.MemType;

public class TestFacets {

  void checkFacets(List<FacetResult> facetVals) {
    assertEquals(1, facetVals.size());
    for (int i = 1; i < facetVals.get(0).labelValues.length; ++i) {
      assertTrue(facetVals.get(0).labelValues[i].value.intValue() <=
          facetVals.get(0).labelValues[i - 1].value.intValue());
    }
  }

  @Test
  public void testFacets() throws Exception {
    testFacets(MemType.Default);
    testFacets(MemType.Heap);
    testFacets(MemType.Direct);
    testFacets(MemType.Native);
  }

  public void testFacets(MemType memType) throws Exception {
    IndexReader reader = FacetTestUtil.getIndexReader(FacetTestUtil.IDX_DIR, memType);
    IndexSearcher searcher = new IndexSearcher(reader);

    TopScoreDocCollector docsCollector = TopScoreDocCollector.create(10, true);
    FacetsCollector facetsCollector = new FacetsCollector(false);

    Collector collector = MultiCollector.wrap(docsCollector, facetsCollector);
    searcher.search(new MatchAllDocsQuery(), collector);

    Facets sizeFacet = new NumericFacetCounts("size", facetsCollector);
    LabelAndOrdFacetCounts colorFacet = new LabelAndOrdFacetCounts("color",
        new SortedDocValuesOrdReader("color"), facetsCollector);

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

    LabelAndOrdFacetCounts tagFacet = new LabelAndOrdFacetCounts("tag",
        new SortedSetDocValuesOrdReader("tag"), facetsCollector);

    TopDocs docs = docsCollector.topDocs();
    assertEquals(reader.numDocs(), docs.totalHits);

    List<FacetResult> facetValues = sizeFacet.getAllDims(2);
    checkFacets(facetValues);
    assertEquals(1, facetValues.size());
    assertEquals(2, facetValues.get(0).labelValues.length);
    assertEquals(3, facetValues.get(0).labelValues[0].value.intValue());
    assertEquals("4", facetValues.get(0).labelValues[0].label);
    assertEquals(2, facetValues.get(0).labelValues[1].value.intValue());
    assertEquals("2", facetValues.get(0).labelValues[1].label);

    facetValues = colorFacet.getAllDims(4);
    checkFacets(facetValues);
    assertEquals(1, facetValues.size());
    assertEquals(3, facetValues.get(0).labelValues.length);
    assertEquals(3, facetValues.get(0).labelValues[0].value.intValue());
    assertEquals("red", facetValues.get(0).labelValues[0].label);
    assertEquals(2, facetValues.get(0).labelValues[1].value.intValue());
    assertEquals("blue", facetValues.get(0).labelValues[1].label);
    assertEquals(2, facetValues.get(0).labelValues[2].value.intValue());
    assertEquals("green", facetValues.get(0).labelValues[2].label);


    facetValues = sizeRangeFacets.getAllDims(3);
    checkFacets(facetValues);
    assertEquals(1, facetValues.size());
    assertEquals(2, facetValues.get(0).labelValues.length);
    assertEquals(4, facetValues.get(0).labelValues[0].value.intValue());
    assertEquals("(3, *)", facetValues.get(0).labelValues[0].label);
    assertEquals(3, facetValues.get(0).labelValues[1].value.intValue());
    assertEquals("(*, 3]", facetValues.get(0).labelValues[1].label);

    facetValues = tagFacet.getAllDims(100);
    assertEquals(1, facetValues.size());
    assertEquals(12, facetValues.get(0).labelValues.length);
    assertEquals(3, facetValues.get(0).labelValues[0].value.intValue());
    assertEquals("funny", facetValues.get(0).labelValues[0].label);
    assertEquals(3, facetValues.get(0).labelValues[1].value.intValue());
    assertEquals("pet", facetValues.get(0).labelValues[1].label);
    assertEquals(3, facetValues.get(0).labelValues[2].value.intValue());
    assertEquals("rabbit", facetValues.get(0).labelValues[2].label);
    assertEquals(2, facetValues.get(0).labelValues[3].value.intValue());
    assertEquals("animal", facetValues.get(0).labelValues[3].label);
    assertEquals(2, facetValues.get(0).labelValues[4].value.intValue());
    assertEquals("cartoon", facetValues.get(0).labelValues[4].label);
    assertEquals(2, facetValues.get(0).labelValues[5].value.intValue());
    assertEquals("dog", facetValues.get(0).labelValues[5].label);
    assertEquals(1, facetValues.get(0).labelValues[6].value.intValue());
    assertEquals("disney", facetValues.get(0).labelValues[6].label);

    reader.close();
  }
}
