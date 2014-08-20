package abacus.search.query;

import abacus.api.AbacusQuery;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;

abstract public class QueryConstructor {

  private static final QueryConstructor RANGE_QUERY_CONSTRUCTOR = new RangeQueryConstructor();
  private static final QueryConstructor STRING_QUERY_CONSTRUCTOR = new StringQueryConstructor();
  private static final QueryConstructor TERM_QUERY_CONSTRUCTOR = new TermQueryConstructor();
  private static final QueryConstructor WILDCARD_QUERY_CONSTRUCTOR = new WildcardQueryConstructor();

  public static Query constructQuery(AbacusQuery abacusQuery, AbacusQueryParser queryParser)
      throws IOException {
    if (abacusQuery == null) {
      return null;
    }
    if (abacusQuery.isSetRangeQuery()) {
      return RANGE_QUERY_CONSTRUCTOR.construct(abacusQuery, queryParser);
    } else if (abacusQuery.isSetStringQuery()) {
      return STRING_QUERY_CONSTRUCTOR.construct(abacusQuery, queryParser);
    } else if (abacusQuery.isSetTermQuery()) {
      return TERM_QUERY_CONSTRUCTOR.construct(abacusQuery, queryParser);
    } else if (abacusQuery.isSetWildcardQuery()) {
      return WILDCARD_QUERY_CONSTRUCTOR.construct(abacusQuery, queryParser);
    }
    return new MatchAllDocsQuery();
  }

  abstract protected Query construct(AbacusQuery abacusQuery, AbacusQueryParser queryParser)
      throws IOException;
}
