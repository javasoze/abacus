package abacus.search.facets;

import junit.framework.TestCase;

import org.junit.Test;

import abacus.api.query.Request;
import abacus.api.query.ResultSet;
import abacus.service.AbacusQueryService;

public class TestFacetsService {

  @Test
  public void testBasic() throws Exception {
    AbacusQueryService svc = new AbacusQueryService(FacetTestUtil.IDX_DIR, FacetTestUtil.QUERY_PARSER);
    Request req = new Request();
    ResultSet results = svc.query(req);
    TestCase.assertEquals(7, results.numHits);
    svc.close();
  }
}
