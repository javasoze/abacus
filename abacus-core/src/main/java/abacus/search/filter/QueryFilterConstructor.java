package abacus.search.filter;

import abacus.api.AbacusFilter;
import abacus.search.query.QueryConstructor;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;

import java.io.IOException;

public class QueryFilterConstructor extends FilterConstructor {
  @Override
  protected Filter construct(AbacusFilter abacusFilter, AbacusQueryParser queryParser)
      throws IOException {
    if (!abacusFilter.isSetQueryFilter()) {
      return null;
    }
    QueryWrapperFilter filter = new QueryWrapperFilter(
        QueryConstructor.constructQuery(abacusFilter.getQueryFilter().getQuery(), queryParser));
    return filter;
  }
}
