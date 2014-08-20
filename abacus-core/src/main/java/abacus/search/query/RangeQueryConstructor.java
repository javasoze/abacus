package abacus.search.query;

import abacus.api.AbacusFieldType;
import abacus.api.AbacusQuery;
import abacus.api.AbacusRange;
import abacus.api.AbacusRangeQuery;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

public class RangeQueryConstructor extends QueryConstructor {
  @Override
  protected Query construct(AbacusQuery abacusQuery, AbacusQueryParser queryParser)
      throws IOException {
    if (!abacusQuery.isSetRangeQuery()) {
      return null;
    }
    AbacusRangeQuery abacusRangeQuery = abacusQuery.getRangeQuery();
    Query query = null;
    AbacusRange range = abacusRangeQuery.getRange();
    AbacusFieldType type = range.getFieldType();
    switch (type) {
    case STRING:
      query = new TermRangeQuery(range.getField(),
          new BytesRef(range.getStartValue()),
          new BytesRef(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    case INT:
      query = NumericRangeQuery.newIntRange(range.getField(),
          Integer.valueOf(range.getStartValue()),
          Integer.valueOf(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    case LONG:
      query = NumericRangeQuery.newLongRange(range.getField(),
          Long.valueOf(range.getStartValue()),
          Long.valueOf(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    case FLOAT:
      query = NumericRangeQuery.newFloatRange(range.getField(),
          Float.valueOf(range.getStartValue()),
          Float.valueOf(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    case DOUBLE:
      query = NumericRangeQuery.newDoubleRange(range.getField(),
          Double.valueOf(range.getStartValue()),
          Double.valueOf(range.getEndValue()),
          range.isStartClosed(),
          range.isEndClosed());
      break;
    }
    return query;
  }
}