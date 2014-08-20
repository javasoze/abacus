package abacus.search.query;

import abacus.api.AbacusQuery;
import abacus.api.AbacusWildcardQuery;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

import java.io.IOException;

public class WildcardQueryConstructor extends QueryConstructor {
  @Override
  protected Query construct(AbacusQuery abacusQuery, AbacusQueryParser queryParser)
      throws IOException {
    if (!abacusQuery.isSetWildcardQuery()) {
      return null;
    }
    AbacusWildcardQuery abacusWildcardQuery = abacusQuery.getWildcardQuery();
    return new WildcardQuery(
        new Term(abacusWildcardQuery.getField(), abacusWildcardQuery.getQuery()));
  }
}
