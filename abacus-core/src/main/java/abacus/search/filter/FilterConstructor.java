package abacus.search.filter;

import abacus.api.AbacusFilter;
import abacus.config.FieldConfig;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.search.Filter;

import java.io.IOException;
import java.util.Map;

public abstract class FilterConstructor {
  private static final FilterConstructor BOOLEAN_FILTER_CONSTRUCTOR = new BooleanFilterConstructor();
  private static final FilterConstructor NULL_FILTER_CONSTRUCTOR = new NullFilterConstructor();
  private static final FilterConstructor QUERY_FILTER_CONSTRUCTOR = new QueryFilterConstructor();
  private static final FilterConstructor RANGE_FILTER_CONSTRUCTOR = new RangeFilterConstructor();
  private static final FilterConstructor TERM_FILTER_CONSTRUCTOR = new TermFilterConstructor();

  public static Filter constructFilter(AbacusFilter abacusFilter, AbacusQueryParser queryParser)
      throws IOException {
    if (abacusFilter == null) {
      return null;
    }
    if (abacusFilter.isSetBooleanFilter()) {
      return BOOLEAN_FILTER_CONSTRUCTOR.construct(abacusFilter, queryParser);
    } else if (abacusFilter.isSetNullFilter()) {
      return NULL_FILTER_CONSTRUCTOR.construct(abacusFilter, queryParser);
    } else if (abacusFilter.isSetQueryFilter()) {
      return QUERY_FILTER_CONSTRUCTOR.construct(abacusFilter, queryParser);
    } else if (abacusFilter.isSetRangeFilter()) {
      return RANGE_FILTER_CONSTRUCTOR.construct(abacusFilter, queryParser);
    } else if (abacusFilter.isSetTermFilter()) {
      return TERM_FILTER_CONSTRUCTOR.construct(abacusFilter, queryParser);
    }
    return null;
  }

  abstract protected Filter construct(AbacusFilter abacusFilter, AbacusQueryParser queryParser)
      throws IOException;
}
