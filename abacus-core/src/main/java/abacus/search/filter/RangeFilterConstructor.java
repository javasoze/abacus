package abacus.search.filter;

import abacus.api.AbacusFieldType;
import abacus.api.AbacusFilter;
import abacus.api.AbacusRange;
import abacus.api.AbacusRangeFilter;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.TermRangeFilter;
import org.apache.lucene.util.BytesRef;

public class RangeFilterConstructor extends FilterConstructor {
  @Override
  protected Filter construct(AbacusFilter abacusFilter, AbacusQueryParser queryParser) {
    if (!abacusFilter.isSetRangeFilter()) {
      return null;
    }
    AbacusRangeFilter abacusRangeFilter = abacusFilter.getRangeFilter();
    AbacusRange range = abacusRangeFilter.getRange();
    AbacusFieldType type = range.getFieldType();
    Filter filter = null;
    switch (type) {
    case STRING:
      BytesRef startBytes = range.getStartValue() == null ?
          null : new BytesRef(range.getStartValue());
      BytesRef endBytes = range.getEndValue() == null ?
          null : new BytesRef(range.getEndValue());
      filter = new TermRangeFilter(range.getField(),
          startBytes,
          endBytes,
          range.isStartClosed(),
          range.isEndClosed());
      break;
    case INT:
      filter = NumericRangeFilter.newIntRange(range.getField(),
          range.getStartValue() == null ? null : Integer.valueOf(range.getStartValue()),
          range.getEndValue() == null ? null : Integer.valueOf(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    case LONG:
      filter = NumericRangeFilter.newLongRange(range.getField(),
          range.getStartValue() == null ? null : Long.valueOf(range.getStartValue()),
          range.getEndValue() == null ? null : Long.valueOf(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    case FLOAT:
      filter = NumericRangeFilter.newFloatRange(range.getField(),
          range.getStartValue() == null ? null : Float.valueOf(range.getStartValue()),
          range.getEndValue() == null ? null : Float.valueOf(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    case DOUBLE:
      filter = NumericRangeFilter.newDoubleRange(range.getField(),
          range.getStartValue() == null ? null : Double.valueOf(range.getStartValue()),
          range.getEndValue() == null ? null : Double.valueOf(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    }
    return filter;
  }
}
