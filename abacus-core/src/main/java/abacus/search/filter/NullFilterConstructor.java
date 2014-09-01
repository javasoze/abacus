package abacus.search.filter;

import abacus.api.AbacusFieldType;
import abacus.api.AbacusFilter;
import abacus.api.AbacusNullFilter;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.TermRangeFilter;

public class NullFilterConstructor extends FilterConstructor {
  @Override
  protected Filter construct(AbacusFilter abacusFilter, AbacusQueryParser queryParser) {
    if (!abacusFilter.isSetNullFilter()) {
      return null;
    }
    AbacusNullFilter nullFilter = abacusFilter.getNullFilter();
    AbacusFieldType type = nullFilter.getFieldType();
    String field = nullFilter.getField();
    Filter filter = null;
    switch (type) {
    case STRING:
      filter = new TermRangeFilter(field, null, null, false, false);
      break;
    case INT:
      filter = NumericRangeFilter
          .newIntRange(field, Integer.MIN_VALUE, Integer.MAX_VALUE, true, true);
      break;
    case LONG:
      filter = NumericRangeFilter.newLongRange(field, Long.MIN_VALUE, Long.MAX_VALUE, true, true);
      break;
    case FLOAT:
      filter = NumericRangeFilter
          .newFloatRange(field, Float.MIN_VALUE, Float.MAX_VALUE, true, true);
      break;
    case DOUBLE:
      filter = NumericRangeFilter
          .newDoubleRange(field, Double.MIN_VALUE, Double.MAX_VALUE, true, true);
      break;
    }
    if (nullFilter.isReverse()) {
      return filter;
    }
    BooleanFilter booleanFilter = new BooleanFilter();
    booleanFilter.add(filter, BooleanClause.Occur.MUST_NOT);
    return booleanFilter;
  }
}
