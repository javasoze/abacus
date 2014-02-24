package abacus.search.facets;

import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;

public interface FacetsFactory {
  Facets createFacets(FacetsCollector collector);
}
