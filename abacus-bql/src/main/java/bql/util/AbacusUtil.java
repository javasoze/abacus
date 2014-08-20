package bql.util;

import abacus.api.AbacusBooleanClauseOccur;
import abacus.api.AbacusBooleanFilter;
import abacus.api.AbacusBooleanQuery;
import abacus.api.AbacusBooleanSubFilter;
import abacus.api.AbacusBooleanSubQuery;
import abacus.api.AbacusFieldType;
import abacus.api.AbacusFilter;
import abacus.api.AbacusQuery;
import abacus.api.AbacusRange;
import abacus.api.AbacusRangeFilter;
import abacus.api.AbacusTermFilter;
import bql.BQLLexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

/**
 * Created by yozhao on 8/13/14.
 */
public class AbacusUtil {

  public static AbacusQuery buildBooleanQuery(List<AbacusQuery> queries,
      AbacusBooleanClauseOccur occur) {
    if (queries == null || queries.isEmpty()) {
      return null;
    }
    if (queries.size() == 1) {
      return queries.get(0);
    }

    AbacusBooleanQuery booleanQuery = new AbacusBooleanQuery();
    for (int i = 0; i < queries.size(); ++i) {
      booleanQuery
          .addToQueries(new AbacusBooleanSubQuery().setQuery(queries.get(i)).setOccur(occur));
    }
    return new AbacusQuery().setBooleanQuery(booleanQuery);
  }

  public static AbacusFilter buildBooleanFilter(List<AbacusFilter> filters,
      AbacusBooleanClauseOccur occur) {
    if (filters == null || filters.isEmpty())
      return null;
    if (filters.size() == 1) {
      return filters.get(0);
    }
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    for (int i = 0; i < filters.size(); ++i) {
      booleanFilter
          .addToFilters(new AbacusBooleanSubFilter().setFilter(filters.get(i)).setOccur(occur));
    }
    return new AbacusFilter().setBooleanFilter(booleanFilter);
  }

  public static String trimStringValue(String value) {
    if (value == null) {
      return null;
    }
    String text = value;
    char initialChar = text.charAt(0);
    if (text.charAt(text.length() - 1) != initialChar) {
      throw new IllegalArgumentException("malformed string literal");
    }
    text = text.substring(1, text.length() - 1);
    if (initialChar == '\'') {
      text = text.replace("''", "'");
    } else if (initialChar == '"') {
      text = text.replace("\"\"", "\"");
    } else {
      throw new UnsupportedOperationException("Not supported yet.");
    }
    return text;
  }

  public static String unescapeStringLiteral(TerminalNode terminalNode) {
    Token token = terminalNode.getSymbol();
    if (token.getType() != BQLLexer.STRING_LITERAL) {
      throw new IllegalArgumentException();
    }

    String text = token.getText();
    char initialChar = text.charAt(0);
    if (text.charAt(text.length() - 1) != initialChar) {
      throw new IllegalArgumentException("malformed string literal");
    }

    text = text.substring(1, text.length() - 1);
    if (initialChar == '\'') {
      text = text.replace("''", "'");
    } else if (initialChar == '"') {
      text = text.replace("\"\"", "\"");
    } else {
      throw new UnsupportedOperationException("Not supported yet.");
    }
    return text;
  }

  public static AbacusFilter buildTermFilter(String field, String value) {
    AbacusTermFilter termFilter = new AbacusTermFilter().setField(field);
    termFilter.addToValues(value);
    return new AbacusFilter().setTermFilter(termFilter);
  }

  public static AbacusFilter buildRangeFilter(String field, String startValue, String endValue,
      boolean isStartClosed, boolean isEndClosed) {
    return new AbacusFilter().setRangeFilter(
        new AbacusRangeFilter(new AbacusRange(field,
            startValue, endValue, isStartClosed,
            isEndClosed, AbacusFieldType.STRING)));
  }

  public static AbacusFilter buildRangeFilter(String field, Integer startValue, Integer endValue,
      boolean isStartClosed, boolean isEndClosed) {
    return new AbacusFilter().setRangeFilter(
        new AbacusRangeFilter(new AbacusRange(field,
            startValue == null ? null : startValue.toString(),
            endValue == null ? null : endValue.toString(), isStartClosed,
            isEndClosed, AbacusFieldType.INT)));
  }

  public static AbacusFilter buildRangeFilter(String field, Long startValue, Long endValue,
      boolean isStartClosed, boolean isEndClosed) {
    return new AbacusFilter().setRangeFilter(
        new AbacusRangeFilter(new AbacusRange(field,
            startValue == null ? null : startValue.toString(),
            endValue == null ? null : endValue.toString(), isStartClosed,
            isEndClosed, AbacusFieldType.LONG)));
  }

  public static AbacusFilter buildRangeFilter(String field, Float startValue, Float endValue,
      boolean isStartClosed, boolean isEndClosed) {
    return new AbacusFilter().setRangeFilter(
        new AbacusRangeFilter(new AbacusRange(field,
            startValue == null ? null : startValue.toString(),
            endValue == null ? null : endValue.toString(), isStartClosed,
            isEndClosed, AbacusFieldType.FLOAT)));
  }

  public static AbacusFilter buildRangeFilter(String field, Double startValue, Double endValue,
      boolean isStartClosed, boolean isEndClosed) {
    return new AbacusFilter().setRangeFilter(
        new AbacusRangeFilter(new AbacusRange(field,
            startValue == null ? null : startValue.toString(),
            endValue == null ? null : endValue.toString(), isStartClosed,
            isEndClosed, AbacusFieldType.DOUBLE)));
  }
}
