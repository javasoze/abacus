package bql.parser;

import abacus.api.AbacusBooleanClauseOccur;
import abacus.api.AbacusBooleanFilter;
import abacus.api.AbacusBooleanSubFilter;
import abacus.api.AbacusFieldType;
import abacus.api.AbacusFilter;
import abacus.api.AbacusNullFilter;
import abacus.api.AbacusQuery;
import abacus.api.AbacusQueryFilter;
import abacus.api.AbacusRequest;
import abacus.api.AbacusSortField;
import abacus.api.AbacusSortFieldType;
import abacus.api.AbacusStringQuery;
import abacus.api.AbacusTermFilter;
import abacus.api.AbacusWildcardQuery;
import abacus.api.FacetParam;
import abacus.api.PagingParam;
import bql.BQLBaseListener;
import bql.BQLParser;
import bql.util.AbacusUtil;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BQLCompilerAnalyzer extends BQLBaseListener {
  private static final int DEFAULT_REQUEST_OFFSET = 0;

  private Map<String, AbacusFieldType> fieldTypeMap = new HashMap<String, AbacusFieldType>();
  private final ParseTreeProperty<AbacusRequest> abacusRequestProperty = new ParseTreeProperty<AbacusRequest>();
  private final ParseTreeProperty<AbacusQuery> queryProperty = new ParseTreeProperty<AbacusQuery>();
  private final ParseTreeProperty<AbacusFilter> filterProperty = new ParseTreeProperty<AbacusFilter>();
  private final ParseTreeProperty<AbacusSortField> sortFieldProperty = new ParseTreeProperty<AbacusSortField>();
  private final ParseTreeProperty<Object> valProperty = new ParseTreeProperty<Object>();
  private final ParseTreeProperty<Integer> offsetProperty = new ParseTreeProperty<Integer>();
  private final ParseTreeProperty<Integer> countProperty = new ParseTreeProperty<Integer>();
  private final List<AbacusSortField> sortFields = new ArrayList<AbacusSortField>();
  private final Map<String, FacetParam> facetParamMap = new HashMap<String, FacetParam>();
  private Boolean inQueryWhere = false;

  public BQLCompilerAnalyzer(Map<String, AbacusFieldType> fieldTypeMap) {
    this.fieldTypeMap = fieldTypeMap;
  }

  public AbacusRequest getAbacusRequest(ParseTree node) {
    return abacusRequestProperty.get(node);
  }

  @Override
  public void exitStatement(BQLParser.StatementContext ctx) {
    if (ctx.select_stmt() != null) {
      abacusRequestProperty.put(ctx, abacusRequestProperty.get(ctx.select_stmt()));
    }
  }

  @Override
  public void exitSelect_stmt(BQLParser.Select_stmtContext ctx) {
    if (ctx.order_by_clause().size() > 1) {
      throw new ParseCancellationException(new SemanticException(ctx.order_by_clause(1),
          "ORDER BY clause can only appear once."));
    }

    if (ctx.limit_clause().size() > 1) {
      throw new ParseCancellationException(new SemanticException(ctx.limit_clause(1),
          "LIMIT clause can only appear once."));
    }

    if (ctx.browse_by_clause().size() > 1) {
      throw new ParseCancellationException(new SemanticException(ctx.browse_by_clause(1),
          "BROWSE BY clause can only appear once."));
    }

    if (ctx.fetching_stored_clause().size() > 1) {
      throw new ParseCancellationException(new SemanticException(ctx.fetching_stored_clause(1),
          "SOURCE clause can only appear once."));
    }

    AbacusRequest abacusRequest = new AbacusRequest();
    if (ctx.limit != null) {
      PagingParam pagingParam = new PagingParam();
      pagingParam.setOffset(offsetProperty.get(ctx.limit));
      pagingParam.setCount(countProperty.get(ctx.limit));
      abacusRequest.setPagingParam(pagingParam);
    }

    if (ctx.fetch_stored != null && (Boolean) valProperty.get(ctx.fetch_stored)) {
      abacusRequest.setFetchSrcData(true);
    }

    if (ctx.explain != null && (Boolean) valProperty.get(ctx.explain)) {
      abacusRequest.setExplain(true);
    }

    if (ctx.q != null) {
      AbacusQuery query = queryProperty.get(ctx.q);
      abacusRequest.setQuery(query);
    }

    if (ctx.w != null) {
      AbacusFilter filter = filterProperty.get(ctx.w);
      abacusRequest.setFilter(filter);
    }

    if (ctx.order_by != null && sortFields.size() > 0) {
      abacusRequest.setSortFields(sortFields);
    }

    if (ctx.browse_by != null && facetParamMap.size() > 0) {
      abacusRequest.setFacetParams(facetParamMap);
    }
    abacusRequestProperty.put(ctx, abacusRequest);
  }

  @Override
  public void enterQuery_where(BQLParser.Query_whereContext ctx) {
    inQueryWhere = true;
  }

  @Override
  public void exitQuery_where(BQLParser.Query_whereContext ctx) {
    inQueryWhere = false;
    queryProperty.put(ctx, queryProperty.get(ctx.search_expr()));
  }

  @Override
  public void exitWhere(BQLParser.WhereContext ctx) {
    filterProperty.put(ctx, filterProperty.get(ctx.search_expr()));
  }

  @Override
  public void exitSearch_expr(BQLParser.Search_exprContext ctx) {
    if (inQueryWhere) {
      if (ctx.term_expr().size() == 1) {
        queryProperty.put(ctx, queryProperty.get(ctx.term_expr(0)));
        return;
      }
      List<AbacusQuery> abacusQueries = new ArrayList<AbacusQuery>();
      for (BQLParser.Term_exprContext f : ctx.term_expr()) {
        AbacusQuery query = queryProperty.get(f);
        if (query == null) {
          continue;
        }
        abacusQueries.add(query);
      }
      AbacusQuery abacusQuery = AbacusUtil.buildBooleanQuery(abacusQueries,
          AbacusBooleanClauseOccur.SHOULD);
      queryProperty.put(ctx, abacusQuery);
    } else {
      if (ctx.term_expr().size() == 1) {
        filterProperty.put(ctx, filterProperty.get(ctx.term_expr(0)));
        return;
      }
      List<AbacusFilter> abacusFilters = new ArrayList<AbacusFilter>();
      for (BQLParser.Term_exprContext f : ctx.term_expr()) {
        AbacusFilter filter = filterProperty.get(f);
        if (filter == null) {
          continue;
        }
        abacusFilters.add(filter);
      }
      AbacusFilter abacusFilter = AbacusUtil.buildBooleanFilter(abacusFilters,
          AbacusBooleanClauseOccur.SHOULD);
      filterProperty.put(ctx, abacusFilter);
    }
  }

  @Override
  public void exitTerm_expr(BQLParser.Term_exprContext ctx) {
    if (inQueryWhere) {
      if (ctx.factor_expr().size() == 1) {
        queryProperty.put(ctx, queryProperty.get(ctx.factor_expr(0)));
        return;
      }
      List<AbacusQuery> abacusQueries = new ArrayList<AbacusQuery>();
      for (BQLParser.Factor_exprContext f : ctx.factor_expr()) {
        AbacusQuery query = queryProperty.get(f);
        if (query == null) {
          continue;
        }
        abacusQueries.add(query);
      }
      AbacusQuery abacusQuery = AbacusUtil.buildBooleanQuery(abacusQueries,
          AbacusBooleanClauseOccur.MUST);
      queryProperty.put(ctx, abacusQuery);
    } else {
      if (ctx.factor_expr().size() == 1) {
        filterProperty.put(ctx, filterProperty.get(ctx.factor_expr(0)));
        return;
      }
      List<AbacusFilter> abacusFilters = new ArrayList<AbacusFilter>();
      for (BQLParser.Factor_exprContext f : ctx.factor_expr()) {
        AbacusFilter filter = filterProperty.get(f);
        if (filter == null) {
          continue;
        }
        abacusFilters.add(filter);
      }
      AbacusFilter abacusFilter = AbacusUtil.buildBooleanFilter(abacusFilters,
          AbacusBooleanClauseOccur.MUST);
      filterProperty.put(ctx, abacusFilter);
    }
  }

  @Override
  public void exitFactor_expr(BQLParser.Factor_exprContext ctx) {
    if (inQueryWhere) {
      if (ctx.predicate() != null) {
        queryProperty.put(ctx, queryProperty.get(ctx.predicate()));
      } else {
        queryProperty.put(ctx, queryProperty.get(ctx.search_expr()));
      }
    } else {
      if (ctx.predicate() != null) {
        filterProperty.put(ctx, filterProperty.get(ctx.predicate()));
      } else {
        filterProperty.put(ctx, filterProperty.get(ctx.search_expr()));
      }
    }
  }

  @Override
  public void exitPredicate(BQLParser.PredicateContext ctx) {
    if (ctx.getChildCount() != 1) {
      throw new UnsupportedOperationException("Not yet implemented");
    }
    if (inQueryWhere) {
      queryProperty.put(ctx, queryProperty.get(ctx.getChild(0)));
    } else {
      filterProperty.put(ctx, filterProperty.get(ctx.getChild(0)));
    }
  }

  @Override
  public void exitEqual_predicate(BQLParser.Equal_predicateContext ctx) {
    String col = ctx.column_name().getText();
    String value = ctx.value().getText();
    AbacusFieldType type = fieldTypeMap.get(col);
    AbacusFilter filter = null;
    if (type != null) {
      switch (type) {
      case STRING:
        value = AbacusUtil.trimStringValue(value);
        filter = AbacusUtil.buildTermFilter(col, value);
        break;
      case INT:
        Integer numI = Integer.valueOf(value);
        filter = AbacusUtil.buildRangeFilter(col, numI, numI, true, true);
        break;
      case LONG:
        Long num = Long.valueOf(value);
        filter = AbacusUtil.buildRangeFilter(col, num, num, true, true);
        break;
      case FLOAT:
        Float numF = Float.valueOf(value);
        filter = AbacusUtil.buildRangeFilter(col, numF, numF, true, true);
        break;
      case DOUBLE:
        Double numD = Double.valueOf(value);
        filter = AbacusUtil.buildRangeFilter(col, numD, numD, true, true);
        break;
      }
      if (filter != null) {
        filterProperty.put(ctx, filter);
      }
    } else {
      throw new ParseCancellationException(
          new SemanticException(ctx, "Schema doesn't has field : " + col));
    }
  }

  @Override
  public void exitNot_equal_predicate(BQLParser.Not_equal_predicateContext ctx) {
    String col = ctx.column_name().getText();
    String value = ctx.value().getText();
    AbacusFieldType type = fieldTypeMap.get(col);

    AbacusFilter filter;
    List<String> values = new ArrayList<String>();
    switch (type) {
    case STRING:
      value = AbacusUtil.trimStringValue(value);
      values.add(value);
      filter = extractStringFilter(col, null, values, AbacusBooleanClauseOccur.SHOULD);
      break;
    default:
      values.add(value);
      filter = extractNumericFilter(col, null, values, type, AbacusBooleanClauseOccur.SHOULD);
    }
    filterProperty.put(ctx, filter);
  }

  @Override
  public void exitBetween_predicate(BQLParser.Between_predicateContext ctx) {
    String col = ctx.column_name().getText();
    AbacusFieldType type = fieldTypeMap.get(col);

    Object val1 = valProperty.get(ctx.val1);
    Object val2 = valProperty.get(ctx.val2);
    if (!checkFieldType(type, val1) || !checkFieldType(type, val2)) {
      throw new ParseCancellationException(
          "Value list for BETWEEN predicate contains incompatible value(s).");
    }
    AbacusFilter filter = null;
    if (type != null) {
      if (ctx.not == null) {
        switch (type) {
        case STRING:
          String strVal1 = AbacusUtil.trimStringValue(val1.toString());
          String strVal2 = AbacusUtil.trimStringValue(val2.toString());
          filter = AbacusUtil.buildRangeFilter(col, strVal1, strVal2, true, true);
          break;
        case INT:
          Integer integer1 = Integer.valueOf(val1.toString());
          Integer integer2 = Integer.valueOf(val2.toString());
          filter = AbacusUtil.buildRangeFilter(col, integer1, integer2, true, true);
          break;
        case LONG:
          Long long1 = Long.valueOf(val1.toString());
          Long long2 = Long.valueOf(val2.toString());
          filter = AbacusUtil.buildRangeFilter(col, long1, long2, true, true);
          break;
        case FLOAT:
          Float float1 = Float.valueOf(val1.toString());
          Float float2 = Float.valueOf(val2.toString());
          filter = AbacusUtil.buildRangeFilter(col, float1, float2, true, true);
          break;
        case DOUBLE:
          Double double1 = Double.valueOf(val1.toString());
          Double double2 = Double.valueOf(val2.toString());
          filter = AbacusUtil.buildRangeFilter(col, double1, double2, true, true);
          break;
        }
      } else {
        AbacusFilter filter1 = null;
        AbacusFilter filter2 = null;
        switch (type) {
        case STRING:
          String strVal1 = AbacusUtil.trimStringValue(val1.toString());
          String strVal2 = AbacusUtil.trimStringValue(val2.toString());
          filter1 = AbacusUtil.buildRangeFilter(col, null, strVal1, false, false);
          filter2 = AbacusUtil.buildRangeFilter(col, strVal2, null, false, false);
          break;
        case INT:
          Integer integer1 = Integer.valueOf(val1.toString());
          Integer integer2 = Integer.valueOf(val2.toString());
          filter1 = AbacusUtil.buildRangeFilter(col, null, integer1, false, false);
          filter2 = AbacusUtil.buildRangeFilter(col, integer2, null, false, false);
          break;
        case LONG:
          Long long1 = Long.valueOf(val1.toString());
          Long long2 = Long.valueOf(val2.toString());
          filter1 = AbacusUtil.buildRangeFilter(col, null, long1, false, false);
          filter2 = AbacusUtil.buildRangeFilter(col, long2, null, false, false);
          break;
        case FLOAT:
          Float float1 = Float.valueOf(val1.toString());
          Float float2 = Float.valueOf(val2.toString());
          filter1 = AbacusUtil.buildRangeFilter(col, null, float1, false, false);
          filter2 = AbacusUtil.buildRangeFilter(col, float2, null, false, false);
          break;
        case DOUBLE:
          Double double1 = Double.valueOf(val1.toString());
          Double double2 = Double.valueOf(val2.toString());
          filter1 = AbacusUtil.buildRangeFilter(col, null, double1, false, false);
          filter2 = AbacusUtil.buildRangeFilter(col, double2, null, false, false);
          break;
        }
        AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
        booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(filter1)
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
        booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(filter2)
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
        filter = new AbacusFilter().setBooleanFilter(booleanFilter);
      }
      if (filter != null) {
        filterProperty.put(ctx, filter);
      }
    } else {
      throw new ParseCancellationException(
          new SemanticException(ctx, "Schema doesn't has field : " + col));
    }
  }

  @Override
  public void exitRange_predicate(BQLParser.Range_predicateContext ctx) {
    String col = ctx.column_name().getText();
    AbacusFieldType type = fieldTypeMap.get(col);
    if (type == null) {
      throw new ParseCancellationException(
          new SemanticException(ctx, "Schema doesn't has field : " + col));
    }

    Object val = valProperty.get(ctx.val);
    if (!checkFieldType(type, val)) {
      throw new ParseCancellationException(
          "Value list for RANGE predicate contains incompatible value(s).");
    }

    AbacusFilter filter = null;
    String strVal1;
    String strVal2;
    boolean isStartClosed = false;
    boolean isEndClosed = false;
    if (ctx.op.getText().charAt(0) == '>') {
      strVal1 = val.toString();
      strVal2 = null;
      if (">=".equals(ctx.op.getText())) {
        isStartClosed = true;
      }
    } else {
      strVal1 = null;
      strVal2 = val.toString();
      if ("<=".equals(ctx.op.getText())) {
        isEndClosed = true;
      }
    }

    if (type != null) {
      switch (type) {
      case STRING:
        filter = AbacusUtil.buildRangeFilter(col, strVal1, strVal2, isStartClosed, isEndClosed);
        break;
      case INT:
        Integer integer1 = strVal1 == null ? null : Integer.valueOf(strVal1);
        Integer integer2 = strVal2 == null ? null : Integer.valueOf(strVal2);
        filter = AbacusUtil.buildRangeFilter(col, integer1, integer2, isStartClosed, isEndClosed);
        break;
      case LONG:
        Long long1 = strVal1 == null ? null : Long.valueOf(strVal1);
        Long long2 = strVal2 == null ? null : Long.valueOf(strVal2);
        filter = AbacusUtil.buildRangeFilter(col, long1, long2, isStartClosed, isEndClosed);
        break;
      case FLOAT:
        Float float1 = strVal1 == null ? null : Float.valueOf(strVal1);
        Float float2 = strVal2 == null ? null : Float.valueOf(strVal2);
        filter = AbacusUtil.buildRangeFilter(col, float1, float2, isStartClosed, isStartClosed);
        break;
      case DOUBLE:
        Double double1 = strVal1 == null ? null : Double.valueOf(strVal1);
        Double double2 = strVal2 == null ? null : Double.valueOf(strVal2);
        filter = AbacusUtil.buildRangeFilter(col, double1, double2, isStartClosed, isStartClosed);
        break;
      }
      if (filter != null) {
        filterProperty.put(ctx, filter);
      }
    } else {
      throw new ParseCancellationException(
          new SemanticException(ctx, "Schema doesn't has field : " + col));
    }
  }

  @Override
  public void exitValue(BQLParser.ValueContext ctx) {
    if (ctx.numeric() != null) {
      valProperty.put(ctx, valProperty.get(ctx.numeric()));
    } else if (ctx.STRING_LITERAL() != null) {
      valProperty.put(ctx, AbacusUtil.unescapeStringLiteral(ctx.STRING_LITERAL()));
    } else if (ctx.TRUE() != null) {
      valProperty.put(ctx, true);
    } else if (ctx.FALSE() != null) {
      valProperty.put(ctx, false);
    } else {
      throw new UnsupportedOperationException("Not yet implemented.");
    }
  }

  @Override
  public void exitNumeric(BQLParser.NumericContext ctx) {
    if (ctx.INTEGER() != null) {
      try {
        valProperty.put(ctx, Long.parseLong(ctx.INTEGER().getText()));
      } catch (NumberFormatException err) {
        throw new ParseCancellationException(new SemanticException(ctx.INTEGER(),
            "Hit NumberFormatException: " + err.getMessage()));
      }
    } else if (ctx.REAL() != null) {
      try {
        valProperty.put(ctx, Double.valueOf(ctx.REAL().getText()));
      } catch (NumberFormatException err) {
        throw new ParseCancellationException(new SemanticException(ctx.INTEGER(),
            "Hit NumberFormatException: " + err.getMessage()));
      }
    } else {
      throw new UnsupportedOperationException("Not yet implemented.");
    }
  }

  @Override
  public void exitQuery_predicate(BQLParser.Query_predicateContext ctx) {
    String orig = AbacusUtil.unescapeStringLiteral(ctx.STRING_LITERAL());
    AbacusQuery stringQuery = new AbacusQuery()
        .setStringQuery(new AbacusStringQuery().setQuery(orig));
    if (inQueryWhere) {
      queryProperty.put(ctx, stringQuery);
    } else {
      AbacusFilter filter = new AbacusFilter().setQueryFilter(
          new AbacusQueryFilter(stringQuery));
      filterProperty.put(ctx, filter);
    }
  }

  @Override
  public void exitNon_variable_value_list(BQLParser.Non_variable_value_listContext ctx) {
    List<Object> objects = new ArrayList<Object>();
    for (BQLParser.ValueContext v : ctx.value()) {
      objects.add(valProperty.get(v));
    }
    valProperty.put(ctx, objects);
  }

  @Override
  public void exitValue_list(BQLParser.Value_listContext ctx) {
    if (ctx.non_variable_value_list() != null) {
      valProperty.put(ctx, valProperty.get(ctx.non_variable_value_list()));
    } else {
      throw new UnsupportedOperationException("Not yet implemented.");
    }
  }

  @Override
  public void exitExcept_clause(BQLParser.Except_clauseContext ctx) {
    valProperty.put(ctx, valProperty.get(ctx.value_list()));
  }

  public void exitLimit_clause(BQLParser.Limit_clauseContext ctx) {
    if (ctx.n1 != null) {
      offsetProperty.put(ctx, Integer.parseInt(ctx.n1.getText()));
    } else {
      offsetProperty.put(ctx, DEFAULT_REQUEST_OFFSET);
    }

    countProperty.put(ctx, Integer.parseInt(ctx.n2.getText()));
  }

  private AbacusFilter extractStringFilter(String col, List<String> values,
      List<String> excepts, AbacusBooleanClauseOccur occur) {
    AbacusTermFilter termFilter = new AbacusTermFilter();
    termFilter.setField(col);
    termFilter.setValues(values);
    termFilter.setExcludes(excepts);
    termFilter.setOccur(occur);
    return new AbacusFilter().setTermFilter(termFilter);
  }

  private AbacusFilter extractNumericFilter(String col, List<String> values,
      List<String> excepts, AbacusFieldType filedType, AbacusBooleanClauseOccur occur) {
    List<AbacusFilter> valueFilters = new ArrayList<AbacusFilter>();
    List<AbacusFilter> exceptFilters = new ArrayList<AbacusFilter>();
    if (values != null) {
      for (String value : values) {
        AbacusFilter filter = null;
        switch (filedType) {
        case INT:
          Integer intNum = Integer.valueOf(value);
          filter = AbacusUtil.buildRangeFilter(col, intNum, intNum, true, true);
          break;
        case LONG:
          Long longNum = Long.valueOf(value);
          filter = AbacusUtil.buildRangeFilter(col, longNum, longNum, true, true);
          break;
        case FLOAT:
          Float floatNum = Float.valueOf(value);
          filter = AbacusUtil.buildRangeFilter(col, floatNum, floatNum, true, true);
          break;
        case DOUBLE:
          Double doubleNum = Double.valueOf(value);
          filter = AbacusUtil.buildRangeFilter(col, doubleNum, doubleNum, true, true);
          break;
        }
        valueFilters.add(filter);
      }
    }
    if (excepts != null) {
      for (String except : excepts) {
        AbacusFilter filter = null;
        switch (filedType) {
        case INT:
          Integer intNum = Integer.valueOf(except);
          filter = AbacusUtil.buildRangeFilter(col, intNum, intNum, true, true);
          break;
        case LONG:
          Long longNum = Long.valueOf(except);
          filter = AbacusUtil.buildRangeFilter(col, longNum, longNum, true, true);
          break;
        case FLOAT:
          Float floatNum = Float.valueOf(except);
          filter = AbacusUtil.buildRangeFilter(col, floatNum, floatNum, true, true);
          break;
        case DOUBLE:
          Double doubleNum = Double.valueOf(except);
          filter = AbacusUtil.buildRangeFilter(col, doubleNum, doubleNum, true, true);
          break;
        }
        exceptFilters.add(filter);
      }
    }

    if (valueFilters.size() == 1 && exceptFilters.size() == 0) {
      return valueFilters.get(0);
    }

    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    for (int i = 0; i < valueFilters.size(); ++i) {
      booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(valueFilters.get(i))
          .setOccur(occur));
    }
    for (int i = 0; i < exceptFilters.size(); ++i) {
      booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(exceptFilters.get(i))
          .setOccur(AbacusBooleanClauseOccur.MUST_NOT));
    }
    return new AbacusFilter().setBooleanFilter(booleanFilter);
  }

  @Override
  public void exitIn_predicate(BQLParser.In_predicateContext ctx) {
    String col = ctx.column_name().getText();
    AbacusFieldType type = fieldTypeMap.get(col);
    if (type == null) {
      throw new ParseCancellationException(
          new SemanticException(ctx, "Schema doesn't has field : " + col));
    }

    List<Object> values = (List<Object>) valProperty.get(ctx.value_list());
    List<Object> excepts = (List<Object>) valProperty.get(ctx.except_clause());
    if (ctx.not != null) {
      List<Object> temp = values;
      values = excepts;
      excepts = temp;
    }

    List<String> strValues = null;
    if (values != null && values.size() > 0) {
      strValues = new ArrayList<String>();
      for (int i = 0; i < values.size(); ++i) {
        if (!checkFieldType(type, values.get(i))) {
          throw new ParseCancellationException(
              "Value list for IN predicate contains incompatible value(s).");
        }
        strValues.add(values.get(i).toString());
      }
    }
    List<String> strExcepts = null;
    if (excepts != null && excepts.size() > 0) {
      strExcepts = new ArrayList<String>();
      for (int i = 0; i < excepts.size(); ++i) {
        if (!checkFieldType(type, excepts.get(i))) {
          throw new ParseCancellationException(
              "Value list for IN predicate contains incompatible value(s).");
        }
        strExcepts.add(excepts.get(i).toString());
      }
    }

    AbacusFilter filter;
    switch (type) {
    case STRING:
      filter = extractStringFilter(col, strValues, strExcepts, AbacusBooleanClauseOccur.SHOULD);
      break;
    default:
      filter = extractNumericFilter(col, strValues, strExcepts, type,
          AbacusBooleanClauseOccur.SHOULD);
    }
    filterProperty.put(ctx, filter);
  }

  @Override
  public void exitContains_all_predicate(BQLParser.Contains_all_predicateContext ctx) {
    String col = ctx.column_name().getText();
    AbacusFieldType type = fieldTypeMap.get(col);
    if (type == null) {
      throw new ParseCancellationException(
          new SemanticException(ctx, "Schema doesn't has field : " + col));
    }

    List<Object> values = (List<Object>) valProperty.get(ctx.value_list());
    List<Object> excepts = (List<Object>) valProperty.get(ctx.except_clause());
    List<String> strValues = null;
    if (values != null) {
      strValues = new ArrayList<String>();
      for (int i = 0; i < values.size(); ++i) {
        if (!checkFieldType(type, values.get(i))) {
          throw new ParseCancellationException(
              "Value list for CONTAINS predicate contains incompatible value(s).");
        }
        strValues.add(values.get(i).toString());
      }
    }
    List<String> strExcepts = null;
    if (excepts != null) {
      strExcepts = new ArrayList<String>();
      for (int i = 0; i < excepts.size(); ++i) {
        if (!checkFieldType(type, excepts.get(i))) {
          throw new ParseCancellationException(
              "Value list for CONTAINS predicate contains incompatible value(s).");
        }
        strExcepts.add(excepts.get(i).toString());
      }
    }
    AbacusFilter filter;
    switch (type) {
    case STRING:
      filter = extractStringFilter(col, strValues, strExcepts, AbacusBooleanClauseOccur.MUST);
      break;
    default:
      filter = extractNumericFilter(col, strValues, strExcepts, type,
          AbacusBooleanClauseOccur.MUST);
    }
    filterProperty.put(ctx, filter);
  }

  @Override
  public void exitSort_spec(BQLParser.Sort_specContext ctx) {
    String col = ctx.column_name().getText();
    AbacusFieldType filedType = fieldTypeMap.get(col);
    AbacusSortFieldType sortFieldType = null;
    if (filedType == AbacusFieldType.STRING) {
      sortFieldType = AbacusSortFieldType.STRING;
    } else if (filedType == AbacusFieldType.INT) {
      sortFieldType = AbacusSortFieldType.INT;
    } else if (filedType == AbacusFieldType.LONG) {
      sortFieldType = AbacusSortFieldType.LONG;
    } else if (filedType == AbacusFieldType.FLOAT) {
      sortFieldType = AbacusSortFieldType.FLOAT;
    } else if (filedType == AbacusFieldType.DOUBLE) {
      sortFieldType = AbacusSortFieldType.DOUBLE;
    }

    if (sortFieldType == null) {
      if (col.equalsIgnoreCase("_score")) {
        sortFieldType = AbacusSortFieldType.SCORE;
      } else {
        throw new ParseCancellationException(
            new SemanticException(ctx, "Schema doesn't has field : " + col));
      }
    }
    AbacusSortField sortField = new AbacusSortField().setField(col).setType(sortFieldType);
    if (ctx.ordering != null && ctx.ordering.getText().toLowerCase().equalsIgnoreCase("desc")) {
      sortField.setReverse(true);
    }
    sortFieldProperty.put(ctx, sortField);
  }

  @Override
  public void exitSort_specs(BQLParser.Sort_specsContext ctx) {
    for (BQLParser.Sort_specContext sort : ctx.sort_spec()) {
      sortFields.add(sortFieldProperty.get(sort));
    }
  }

  @Override
  public void exitFacet_spec(BQLParser.Facet_specContext ctx) {
    FacetParam facetParam = new FacetParam();
    String col = ctx.column_name().getText();
    if (ctx.n1 != null) {
      int maxNum = Integer.parseInt(ctx.n1.getText());
      facetParam.setMaxNumValues(maxNum);
    }
    facetParamMap.put(col, facetParam);
  }

  @Override
  public void exitLike_predicate(BQLParser.Like_predicateContext ctx) {
    String col = ctx.column_name().getText();
    AbacusFieldType filedType = fieldTypeMap.get(col);
    if (filedType != AbacusFieldType.STRING) {
      throw new ParseCancellationException(new SemanticException(ctx.column_name(),
          "Non-string type column \"" + col + "\" cannot be used in LIKE predicates."));
    }

    String orig = AbacusUtil.unescapeStringLiteral(ctx.STRING_LITERAL());
    String likeString = orig.replace('%', '*').replace('_', '?');

    AbacusWildcardQuery wildcardQuery = new AbacusWildcardQuery().setField(col)
        .setQuery(likeString);
    if (inQueryWhere) {
      if (ctx.NOT() != null) {
        throw new ParseCancellationException(new SemanticException(ctx.column_name(),
            "NOT LIKE can not be used in QUERY predicates."));
      }
      queryProperty.put(ctx, new AbacusQuery().setWildcardQuery(wildcardQuery));
    } else {
      AbacusFilter filter = new AbacusFilter().setQueryFilter(
          new AbacusQueryFilter().setQuery(new AbacusQuery().setWildcardQuery(wildcardQuery)));
      if (ctx.NOT() != null) {
        AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
        booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(filter)
            .setOccur(AbacusBooleanClauseOccur.MUST_NOT));
        filter = new AbacusFilter().setBooleanFilter(booleanFilter);
      }
      filterProperty.put(ctx, filter);
    }
  }

  @Override
  public void exitNull_predicate(BQLParser.Null_predicateContext ctx) {
    String col = ctx.column_name().getText();
    AbacusFieldType filedType = fieldTypeMap.get(col);
    if (filedType == null) {
      throw new ParseCancellationException(
          new SemanticException(ctx, "Schema doesn't has field : " + col));
    }

    AbacusNullFilter nullFilter = new AbacusNullFilter().setField(col).setFieldType(filedType);
    if (ctx.NOT() != null) {
      nullFilter.setReverse(true);
    }
    filterProperty.put(ctx, new AbacusFilter().setNullFilter(nullFilter));
  }

  @Override
  public void exitExplain_clause(BQLParser.Explain_clauseContext ctx) {
    valProperty.put(ctx, ctx.FALSE().isEmpty());
  }

  @Override
  public void exitFetching_stored_clause(BQLParser.Fetching_stored_clauseContext ctx) {
    valProperty.put(ctx, ctx.FALSE().isEmpty());
  }

  public boolean checkFieldType(AbacusFieldType type, Object value) {
    if (type == AbacusFieldType.STRING && value instanceof String) {
      return true;
    } else if ((type == AbacusFieldType.INT || type == AbacusFieldType.LONG)
        && (value instanceof Integer || value instanceof Long)) {
      return true;
    } else if ((type == AbacusFieldType.FLOAT || type == AbacusFieldType.DOUBLE)
        && (value instanceof Float || value instanceof Double)) {
      return true;
    }
    return false;
  }
}
