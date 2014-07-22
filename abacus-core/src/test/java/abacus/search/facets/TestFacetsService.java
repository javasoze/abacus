package abacus.search.facets;

import abacus.api.AbacusRequest;
import abacus.api.AbacusResult;
import abacus.service.AbacusQueryService;
import junit.framework.TestCase;
import org.junit.Test;

public class TestFacetsService {

  @Test
  public void testBasic() throws Exception {
    AbacusQueryService svc = new AbacusQueryService(FacetTestUtil.IDX_DIR, FacetTestUtil.QUERY_PARSER);
    AbacusRequest req = new AbacusRequest();
    AbacusResult results = svc.query(req);
    TestCase.assertEquals(7, results.getNumHits());
    svc.close();
  }
}
