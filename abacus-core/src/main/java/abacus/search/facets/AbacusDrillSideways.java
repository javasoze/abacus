package abacus.search.facets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.facet.DrillSideways;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.MultiFacets;
import org.apache.lucene.search.IndexSearcher;

public class AbacusDrillSideways extends DrillSideways {
  
  private final FacetsFactory facetsFactory;
  
  public AbacusDrillSideways(IndexSearcher searcher, FacetsConfig config, FacetsFactory facetsFactory) {
    super(searcher, config, null, null);
    this.facetsFactory = facetsFactory;
  }

  @Override
  protected Facets buildFacetsResult(FacetsCollector drillDowns, 
      FacetsCollector[] drillSideways, String[] drillSidewaysDims) throws IOException {
    
    Facets drillDownFacets;
    Map<String,Facets> drillSidewaysFacets = new HashMap<String,Facets>();

    drillDownFacets = facetsFactory.createFacets(drillDowns);
    
    if (drillSideways != null) {
      for(int i=0;i<drillSideways.length;i++) {
        drillSidewaysFacets.put(drillSidewaysDims[i],
            facetsFactory.createFacets(drillSideways[i]));
      }
    }

    if (drillSidewaysFacets.isEmpty()) {
      return drillDownFacets;
    } else {
      return new MultiFacets(drillSidewaysFacets, drillDownFacets);
    }
  }

}
