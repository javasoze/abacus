package abacus.search.query;

import abacus.api.AbacusQuery;
import abacus.api.AbacusTermQuery;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;

public class TermQueryConstructor extends QueryConstructor {

  @Override
  protected Query construct(AbacusQuery abacusQuery, AbacusQueryParser queryParser)
      throws IOException {
    if (!abacusQuery.isSetTermQuery()) {
      return null;
    }
    AbacusTermQuery abacusTermQuery = abacusQuery.getTermQuery();
    return new TermQuery(new Term(abacusTermQuery.getField(), abacusTermQuery.getValue()));
  }

}