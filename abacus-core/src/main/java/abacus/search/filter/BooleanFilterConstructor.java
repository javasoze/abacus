package abacus.search.filter;

import abacus.api.AbacusBooleanClauseOccur;
import abacus.api.AbacusBooleanFilter;
import abacus.api.AbacusBooleanSubFilter;
import abacus.api.AbacusFilter;
import abacus.config.FieldConfig;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Filter;

import java.io.IOException;
import java.util.Map;

public class BooleanFilterConstructor extends FilterConstructor {
  @Override
  protected Filter construct(AbacusFilter abacusFilter, AbacusQueryParser queryParser)
      throws IOException {
    if (!abacusFilter.isSetBooleanFilter()) {
      return null;
    }
    AbacusBooleanFilter abacusBooleanFilter = abacusFilter.getBooleanFilter();
    if (abacusBooleanFilter.getFiltersSize() == 0) {
      return null;
    }
    BooleanFilter filter = new BooleanFilter();
    for (int i = 0; i < abacusBooleanFilter.getFiltersSize(); ++i) {
      AbacusBooleanSubFilter subFilter = abacusBooleanFilter.getFilters().get(i);
      BooleanClause.Occur occur = BooleanClause.Occur.SHOULD;
      if (subFilter.getOccur() == AbacusBooleanClauseOccur.MUST) {
        occur = BooleanClause.Occur.MUST;
      } else if (subFilter.getOccur() == AbacusBooleanClauseOccur.MUST_NOT) {
        occur = BooleanClause.Occur.MUST_NOT;
      }
      filter.add(FilterConstructor.constructFilter(subFilter.getFilter(), queryParser), occur);
    }
    return filter;
  }
}
