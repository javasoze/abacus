package abacus.search.filter;

import abacus.api.AbacusFieldType;
import abacus.api.AbacusFilter;
import abacus.api.AbacusNullFilter;
import abacus.config.FieldConfig;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.WildcardQuery;

import java.util.Map;

public class NullFilterConstructor extends FilterConstructor {
  @Override
  protected Filter construct(AbacusFilter abacusFilter, AbacusQueryParser queryParser) {
    if (!abacusFilter.isSetNullFilter()) {
      return null;
    }
    AbacusNullFilter nullFilter = abacusFilter.getNullFilter();
    AbacusFieldType type = nullFilter.getFieldType();
    String field = nullFilter.getField();
    QueryWrapperFilter queryFilter = null;
    switch (type) {
    case STRING:
      queryFilter = new QueryWrapperFilter(new WildcardQuery(new Term(field, "*")));
      break;
    case INT:
      queryFilter = new QueryWrapperFilter(NumericRangeQuery.newIntRange(field,
          Integer.MIN_VALUE, Integer.MAX_VALUE, true, true));
      break;
    case LONG:
      queryFilter = new QueryWrapperFilter(NumericRangeQuery.newLongRange(field,
          Long.MIN_VALUE, Long.MAX_VALUE, true, true));
      break;
    case FLOAT:
      queryFilter = new QueryWrapperFilter(NumericRangeQuery.newFloatRange(field,
          Float.MIN_VALUE, Float.MAX_VALUE, true, true));
      break;
    case DOUBLE:
      queryFilter = new QueryWrapperFilter(NumericRangeQuery.newDoubleRange(field,
          Double.MIN_VALUE, Double.MAX_VALUE, true, true));
      break;
    }
    if (nullFilter.isReverse()) {
      return queryFilter;
    }
    BooleanFilter booleanFilter = new BooleanFilter();
    booleanFilter.add(queryFilter, BooleanClause.Occur.MUST_NOT);
    return booleanFilter;
  }
}
