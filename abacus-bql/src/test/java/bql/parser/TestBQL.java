package bql.parser;

import abacus.api.AbacusBooleanClauseOccur;
import abacus.api.AbacusBooleanFilter;
import abacus.api.AbacusBooleanSubFilter;
import abacus.api.AbacusFieldType;
import abacus.api.AbacusFilter;
import abacus.api.AbacusNullFilter;
import abacus.api.AbacusQuery;
import abacus.api.AbacusQueryFilter;
import abacus.api.AbacusRange;
import abacus.api.AbacusRangeFilter;
import abacus.api.AbacusRequest;
import abacus.api.AbacusSortField;
import abacus.api.AbacusSortFieldType;
import abacus.api.AbacusStringQuery;
import abacus.api.AbacusTermFilter;
import abacus.api.AbacusWildcardQuery;
import abacus.api.FacetParam;
import abacus.api.PagingParam;
import bql.util.AbacusUtil;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

public class TestBQL {

  private final BQLCompiler _compiler;

  @Rule
  public final ExpectedException expectedEx = ExpectedException.none();

  public TestBQL() {
    super();
    Map<String, AbacusFieldType> configMap = new HashMap<String, AbacusFieldType>();
    configMap.put("tags", AbacusFieldType.STRING);
    configMap.put("category", AbacusFieldType.STRING);
    configMap.put("price", AbacusFieldType.FLOAT);
    configMap.put("mileage", AbacusFieldType.INT);
    configMap.put("color", AbacusFieldType.STRING);
    configMap.put("year", AbacusFieldType.INT);
    configMap.put("long_id", AbacusFieldType.LONG);
    configMap.put("groupid_range", AbacusFieldType.LONG);
    configMap.put("name", AbacusFieldType.STRING);
    _compiler = new BQLCompiler(configMap);
  }

  @Test
  public void testOrderBy() throws Exception {
    System.out.println("testOrderBy");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "ORDER BY color";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.addToSortFields(
        new AbacusSortField().setField("color").setType(AbacusSortFieldType.STRING));
    assertEquals(expected, request);
  }

  @Test
  public void testOrderBy2() throws Exception {
    System.out.println("testOrderBy2");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars "
        + "ORDER BY color, price DESC, year ASC";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.addToSortFields(
        new AbacusSortField().setField("color").setType(AbacusSortFieldType.STRING));
    expected.addToSortFields(
        new AbacusSortField().setField("price").setType(AbacusSortFieldType.FLOAT)
            .setReverse(true));
    expected.addToSortFields(
        new AbacusSortField().setField("year").setType(AbacusSortFieldType.INT));
    assertEquals(expected, request);
  }

  @Test
  public void testOrderByRelevance() throws Exception {
    System.out.println("testOrderByRelevance");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "ORDER BY _score";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.addToSortFields(
        new AbacusSortField().setField("_score").setType(AbacusSortFieldType.SCORE));
    assertEquals(expected, request);
  }

  @Test
  public void testLimit1() throws Exception {
    System.out.println("testLimit1");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "LIMIT 123";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.setPagingParam(new PagingParam().setOffset(0).setCount(123));
    assertEquals(expected, request);
  }

  @Test
  public void testLimit2() throws Exception {
    System.out.println("testLimit2");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "LIMIT 15, 30";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.setPagingParam(new PagingParam().setOffset(15).setCount(30));
    assertEquals(expected, request);
  }

  @Test
  public void testExplain() throws Exception {
    System.out.println("testExplain");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars EXPLAIN " + "LIMIT 15, 30";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.setPagingParam(new PagingParam().setOffset(15).setCount(30));
    expected.setExplain(true);
    assertEquals(expected, request);
  }

  @Test
  public void testEqualPredInteger() throws Exception {
    System.out.println("testEqualPredInteger");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE year = 1999";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.setFilter(new AbacusFilter()
        .setRangeFilter(new AbacusRangeFilter().setRange(
            new AbacusRange().setField("year").setStartValue("1999").setEndValue("1999")
                .setStartClosed(true).setEndClosed(true).setFieldType(AbacusFieldType.INT))));
    assertEquals(expected, request);
  }

  @Test
  public void testEqualPredFloat() throws Exception {
    System.out.println("testEqualPredFloat");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE price = 1500.99";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.setFilter(new AbacusFilter()
        .setRangeFilter(new AbacusRangeFilter().setRange(
            new AbacusRange().setField("price").setStartValue("1500.99").setEndValue("1500.99")
                .setStartClosed(true).setEndClosed(true).setFieldType(AbacusFieldType.FLOAT))));
    assertEquals(expected, request);
  }

  @Test
  public void testEqualPredString() throws Exception {
    System.out.println("testEqualPredString");
    System.out.println("==================================================");

    String bql = "SELECT category " + "FROM cars " + "WHERE color = 'red'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter = new AbacusTermFilter();
    termFilter.setField("color");
    termFilter.addToValues("red");
    expected.setFilter(new AbacusFilter().setTermFilter(termFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testInPred1() throws Exception {
    System.out.println("testInPred1");
    System.out.println("==================================================");

    String bql = "SELECT category " + "FROM cars " + "WHERE color IN ('red', 'blue', 'yellow')";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter = new AbacusTermFilter();
    termFilter.setField("color");
    termFilter.addToValues("red");
    termFilter.addToValues("blue");
    termFilter.addToValues("yellow");
    expected.setFilter(new AbacusFilter().setTermFilter(termFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testInPred2() throws Throwable {
    System.out.println("testInPred2");
    System.out.println("==================================================");

    expectedEx.expect(SemanticException.class);
    expectedEx.expectMessage("Schema doesn't has field : nonexist");
    String bql = "SELECT category " + "FROM cars " + "WHERE nonexist IN ('red')";
    try {
      _compiler.compile(bql);
    } catch (ParseCancellationException ex) {
      throw ex.getCause();
    }
  }

  @Test
  public void testInPred3() throws Throwable {
    System.out.println("testInPred3");
    System.out.println("==================================================");

    String bql = "SELECT * \n" + "FROM cars \n"
        + "WHERE year IN (1995,2000) " + "  AND color = 'red'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter = new AbacusTermFilter();
    termFilter.setField("color");
    termFilter.addToValues("red");

    AbacusBooleanFilter inFilters = new AbacusBooleanFilter();
    inFilters.addToFilters(
        new AbacusBooleanSubFilter()
            .setFilter(new AbacusFilter().setRangeFilter(new AbacusRangeFilter()
                .setRange(
                    new AbacusRange("year", "1995", "1995", true, true, AbacusFieldType.INT))))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    inFilters.addToFilters(
        new AbacusBooleanSubFilter()
            .setFilter(new AbacusFilter().setRangeFilter(new AbacusRangeFilter()
                .setRange(
                    new AbacusRange("year", "2000", "2000", true, true, AbacusFieldType.INT))))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));

    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setBooleanFilter(inFilters))
            .setOccur(
                AbacusBooleanClauseOccur.MUST));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter))
            .setOccur(
                AbacusBooleanClauseOccur.MUST));

    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testNotInPred() throws Exception {
    System.out.println("testNotInPred");
    System.out.println("==================================================");

    String bql = "SELECT category " + "FROM cars "
        + "WHERE color NOT IN ('red', 'blue', 'yellow') EXCEPT ('black', 'green')";

    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter = new AbacusTermFilter();
    termFilter.setField("color");
    termFilter.addToValues("black");
    termFilter.addToValues("green");
    termFilter.addToExcludes("red");
    termFilter.addToExcludes("blue");
    termFilter.addToExcludes("yellow");
    expected.setFilter(new AbacusFilter().setTermFilter(termFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testContainsAll() throws Exception {
    System.out.println("testContainsAll");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars "
        + "WHERE color CONTAINS ALL ('red', 'blue', 'yellow') EXCEPT ('black', 'green')";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter = new AbacusTermFilter();
    termFilter.setField("color");
    termFilter.addToValues("red");
    termFilter.addToValues("blue");
    termFilter.addToValues("yellow");
    termFilter.addToExcludes("black");
    termFilter.addToExcludes("green");
    termFilter.setOccur(AbacusBooleanClauseOccur.MUST);
    expected.setFilter(new AbacusFilter().setTermFilter(termFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testNotEqualPred() throws Exception {
    System.out.println("testNotEqualPred");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE color <> 'red'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter = new AbacusTermFilter().setField("color");
    termFilter.addToExcludes("red");
    expected.setFilter(new AbacusFilter().setTermFilter(termFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testNotEqualForInt() throws Exception {
    System.out.println("testNotEqualForInt");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE year <> 2000";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter rangeFilter = AbacusUtil.buildRangeFilter("year", 2000, 2000, true, true);
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter
        .addToFilters(new AbacusBooleanSubFilter().setOccur(AbacusBooleanClauseOccur.MUST_NOT)
            .setFilter(rangeFilter));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testQueryIs() throws Exception {
    System.out.println("testQueryIs");
    System.out.println("==================================================");

    String bql = "SELECT *  FROM cars QUERY query is 'cool AND moon-roof'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusQuery query = new AbacusQuery()
        .setStringQuery(new AbacusStringQuery().setQuery("cool AND moon-roof"));
    expected.setQuery(query);
    assertEquals(expected, request);
  }

  @Test
  public void testQueryFilter() throws Exception {
    System.out.println("testQueryFilter");
    System.out.println("==================================================");

    String bql = "SELECT *  " + "FROM cars " + "WHERE QUERY IS 'cool AND moon-roof'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusQuery query = new AbacusQuery()
        .setStringQuery(new AbacusStringQuery().setQuery("cool AND moon-roof"));
    AbacusQueryFilter queryFilter = new AbacusQueryFilter().setQuery(query);
    expected.setFilter(new AbacusFilter().setQueryFilter(queryFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testQueryAndSelection1() throws Exception {
    System.out.println("testQueryAndSelection1");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars "
        + "QUERY query IS 'cool AND moon-roof' "
        + "WHERE color = 'red'                                -- LINE COMMENTS\n"
        + "AND category = 'sedan'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusQuery query = new AbacusQuery()
        .setStringQuery(new AbacusStringQuery().setQuery("cool AND moon-roof"));
    expected.setQuery(query);
    AbacusTermFilter termFilter1 = new AbacusTermFilter().setField("color");
    termFilter1.addToValues("red");
    AbacusTermFilter termFilter2 = new AbacusTermFilter().setField("category");
    termFilter2.addToValues("sedan");
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter1))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter2))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testQueryAndSelection2() throws Exception {
    System.out.println("testQueryAndSelection2");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE QUERY IS 'cool AND moon-roof' "
        + "AND year = 12 ";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusQuery query = new AbacusQuery()
        .setStringQuery(new AbacusStringQuery().setQuery("cool AND moon-roof"));
    AbacusQueryFilter queryFilter = new AbacusQueryFilter().setQuery(query);
    AbacusFilter rangeFilter = AbacusUtil.buildRangeFilter("year", 12, 12, true, true);
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setQueryFilter(queryFilter))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(rangeFilter)
            .setOccur(AbacusBooleanClauseOccur.MUST));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testBrowseBy1() throws Exception {
    System.out.println("testBrowseBy1");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "BROWSE BY color";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    Map<String, FacetParam> mapFacetParam = new HashMap<String, FacetParam>();
    mapFacetParam.put("color", new FacetParam());
    expected.setFacetParams(mapFacetParam);
    assertEquals(expected, request);
  }

  @Test
  public void testBrowseBy2() throws Exception {
    System.out.println("testBrowseBy2");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars "
        + "BROWSE BY color, price(20), year";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    Map<String, FacetParam> mapFacetParam = new HashMap<String, FacetParam>();
    mapFacetParam.put("color", new FacetParam());
    mapFacetParam.put("price", new FacetParam().setMaxNumValues(20));
    mapFacetParam.put("year", new FacetParam());
    expected.setFacetParams(mapFacetParam);
    assertEquals(expected, request);
  }

  @Test
  public void testBetweenPred() throws Exception {
    System.out.println("testBetweenPred");
    System.out.println("==================================================");

    String bql = "SELECT category " + "FROM cars "
        + "WHERE year BETWEEN 2000 AND 2001";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter filter = AbacusUtil.buildRangeFilter("year", 2000, 2001, true, true);
    expected.setFilter(filter);
    assertEquals(expected, request);
  }

  @Test
  public void testFetchingStored1() throws Exception {
    System.out.println("testFetchingStored1");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "FETCHING STORED FALSE";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    assertEquals(expected, request);
  }

  @Test
  public void testFetchingStored2() throws Exception {
    System.out.println("testFetchingStored2");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "FETCHING STORED true";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.setFetchSrcData(true);
    assertEquals(expected, request);
  }

  @Test
  public void testNotBetweenPred() throws Exception {
    System.out.println("testNotBetweenPred");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE year NOT BETWEEN 2000 AND 2002";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter filter1 = AbacusUtil.buildRangeFilter("year", null, 2000, false, false);
    AbacusFilter filter2 = AbacusUtil.buildRangeFilter("year", 2002, null, false, false);
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(filter1)
        .setOccur(AbacusBooleanClauseOccur.SHOULD));
    booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(filter2)
        .setOccur(AbacusBooleanClauseOccur.SHOULD));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testRangePred1() throws Exception {
    System.out.println("testRangePred1");
    System.out.println("==================================================");
    String bql = "SELECT * " + "FROM cars " + "WHERE year > 1999";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter filter = AbacusUtil.buildRangeFilter("year", 1999, null, false, false);
    expected.setFilter(filter);
    assertEquals(expected, request);
  }

  @Test
  public void testRangePred2() throws Exception {
    System.out.println("testRangePred2");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE year <= 2000";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter filter = AbacusUtil.buildRangeFilter("year", null, 2000, false, true);
    expected.setFilter(filter);
    assertEquals(expected, request);
  }

  @Test
  public void testRangePred3() throws Exception {
    System.out.println("testRangePred3");
    System.out.println("==================================================");

    String bql =
        "SELECT * " + "FROM cars " + "WHERE year > 1999 AND year <= 2003 AND year >= 1999";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter filter1 = AbacusUtil.buildRangeFilter("year", 1999, null, false, false);
    AbacusFilter filter2 = AbacusUtil.buildRangeFilter("year", null, 2003, false, true);
    AbacusFilter filter3 = AbacusUtil.buildRangeFilter("year", 1999, null, true, false);
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(filter1).setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(filter2).setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(filter3).setOccur(AbacusBooleanClauseOccur.MUST));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testRangePred4() throws Exception {
    System.out.println("testRangePred4");
    System.out.println("==================================================");

    String bql =
        "SELECT * " + "FROM cars " + "WHERE name > 'abc' AND name < 'xyz' AND name >= 'ddd'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter filter1 = AbacusUtil.buildRangeFilter("name", "abc", null, false, false);
    AbacusFilter filter2 = AbacusUtil.buildRangeFilter("name", null, "xyz", false, false);
    AbacusFilter filter3 = AbacusUtil.buildRangeFilter("name", "ddd", null, true, false);
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(filter1).setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(filter2).setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(filter3).setOccur(AbacusBooleanClauseOccur.MUST));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testOrPred() throws Exception {
    System.out.println("testOrPred");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE color = 'red' OR year > 1995";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter rangeFilter = AbacusUtil.buildRangeFilter("year", 1995, null, false, false);
    AbacusTermFilter termFilter = new AbacusTermFilter().setField("color");
    termFilter.addToValues("red");
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(rangeFilter)
        .setOccur(AbacusBooleanClauseOccur.SHOULD));
    assertEquals(expected, request);
  }

  @Test
  public void testAndPred() throws Exception {
    System.out.println("testAndPred");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE color = 'red' AND year > 1995";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter rangeFilter = AbacusUtil.buildRangeFilter("year", 1995, null, false, false);
    AbacusTermFilter termFilter = new AbacusTermFilter().setField("color");
    termFilter.addToValues("red");
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(rangeFilter)
        .setOccur(AbacusBooleanClauseOccur.MUST));
    assertEquals(expected, request);
  }

  @Test
  public void testAndOrPred() throws Exception {
    System.out.println("testAndOrPred");
    System.out.println("==================================================");

    String bql = "SELECT color " + "FROM cars "
        + "WHERE (color = 'red' OR color = 'blue') "
        + "   OR (color = 'black' AND tags CONTAINS ALL ('hybrid', 'moon-roof', 'leather'))";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter1 = new AbacusTermFilter().setField("color");
    termFilter1.addToValues("red");
    AbacusTermFilter termFilter2 = new AbacusTermFilter().setField("color");
    termFilter2.addToValues("blue");
    AbacusBooleanFilter booleanFilter1 = new AbacusBooleanFilter();
    booleanFilter1.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter1))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    booleanFilter1.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter2))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    AbacusTermFilter termFilter3 = new AbacusTermFilter().setField("color");
    termFilter3.addToValues("black");
    AbacusTermFilter termFilter4 = new AbacusTermFilter().setField("tags");
    termFilter4.addToValues("hybrid");
    termFilter4.addToValues("moon-roof");
    termFilter4.addToValues("leather");
    termFilter4.setOccur(AbacusBooleanClauseOccur.MUST);
    AbacusBooleanFilter booleanFilter2 = new AbacusBooleanFilter();
    booleanFilter2.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter3))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter2.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter4))
            .setOccur(AbacusBooleanClauseOccur.MUST));

    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setBooleanFilter(booleanFilter1))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setBooleanFilter(booleanFilter2))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testAndFilter() throws Exception {
    System.out.println("testSelectionAndFilter");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE color = 'red' AND year > 25";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter = new AbacusTermFilter().setField("color");
    termFilter.addToValues("red");
    AbacusFilter rangeFilter = AbacusUtil.buildRangeFilter("year", 25, null, false, false);
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter.addToFilters(new AbacusBooleanSubFilter().setFilter(rangeFilter)
        .setOccur(AbacusBooleanClauseOccur.MUST));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testMultipleQueries() throws Exception {
    System.out.println("testMultipleQueries");
    System.out.println("==================================================");

    String bql = "SELECT color " + "FROM cars "
        + "WHERE (color = 'red' AND query is 'hybrid AND cool') "
        + "   OR (color = 'blue' AND query is 'moon-roof AND navigation')";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusTermFilter termFilter1 = new AbacusTermFilter().setField("color");
    termFilter1.addToValues("red");
    AbacusQueryFilter queryFilter1 = new AbacusQueryFilter()
        .setQuery(new AbacusQuery().setStringQuery(new AbacusStringQuery().setQuery(
            "hybrid AND cool")));
    AbacusTermFilter termFilter2 = new AbacusTermFilter().setField("color");
    termFilter2.addToValues("blue");
    AbacusQueryFilter queryFilter2 = new AbacusQueryFilter()
        .setQuery(new AbacusQuery().setStringQuery(new AbacusStringQuery().setQuery(
            "moon-roof AND navigation")));
    AbacusBooleanFilter booleanFilter1 = new AbacusBooleanFilter();
    booleanFilter1.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter1))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter1.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setQueryFilter(queryFilter1))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    AbacusBooleanFilter booleanFilter2 = new AbacusBooleanFilter();
    booleanFilter2.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter2))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter2.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setQueryFilter(queryFilter2))
            .setOccur(AbacusBooleanClauseOccur.MUST));

    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setBooleanFilter(booleanFilter1))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setBooleanFilter(booleanFilter2))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));

    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));

    assertEquals(expected, request);
  }

  @Test
  public void testLikePredicate1() throws Exception {
    System.out.println("testLikePredicate1");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE category LIKE 's_d%'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter filter = new AbacusFilter()
        .setQueryFilter(new AbacusQueryFilter().setQuery(new AbacusQuery()
            .setWildcardQuery(new AbacusWildcardQuery().setQuery("s?d*").setField("category"))));
    expected.setFilter(filter);
    assertEquals(expected, request);
  }

  @Test
  public void testLikeQueryPredicate2() throws Exception {
    System.out.println("testLikePredicate2");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "QUERY category LIKE 'sed*'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusQuery query = new AbacusQuery();
    query.setWildcardQuery(new AbacusWildcardQuery().setQuery("sed*").setField("category"));
    expected.setQuery(query);
    assertEquals(expected, request);
  }

  @Test
  public void testNotLikePredicate() throws Exception {
    System.out.println("testNotLikePredicate");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE color NOT LIKE 'bl%'";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusQuery query = new AbacusQuery();
    query.setWildcardQuery(new AbacusWildcardQuery().setQuery("bl*").setField("color"));
    AbacusFilter filter = new AbacusFilter()
        .setQueryFilter(new AbacusQueryFilter().setQuery(query));
    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(filter).setOccur(AbacusBooleanClauseOccur.MUST_NOT));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    assertEquals(expected, request);
  }

  @Test
  public void testColumnType4() throws Throwable {
    System.out.println("testColumnType4");
    System.out.println("==================================================");

    expectedEx.expect(ParseCancellationException.class);
    expectedEx.expectMessage(
        "Value list for CONTAINS predicate contains incompatible value(s).");
    String bql = "SELECT * " + "FROM cars " + "WHERE tags CONTAINS ALL ('cool', 123)";
    _compiler.compile(bql);
  }

  @Test
  public void testLongValue() throws Exception {
    System.out.println("testLongValue");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE long_id IN (5497057336205783040)";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusFilter abacusFilter = AbacusUtil
        .buildRangeFilter("long_id", 5497057336205783040L, 5497057336205783040L, true, true);
    expected.setFilter(abacusFilter);
    assertEquals(expected, request);
  }

  @Test
  public void testCorrectStatement() throws Exception {
    System.out.println("testCorrectStatement");
    System.out.println("==================================================");
    // compile the statement

    String bql = "SELECT * " + "FROM cars "
        + "QUERY query IS \"hello\" " + "  WHERE color IN (\"red\", \"blue\") EXCEPT ('red') "
        + "  OR category = 'sedan' AND year NOT BETWEEN 1999 AND 2000";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    AbacusQuery query = new AbacusQuery().setStringQuery(new AbacusStringQuery().setQuery("hello"));
    AbacusTermFilter termFilter1 = new AbacusTermFilter().setField("color");
    termFilter1.addToValues("red");
    termFilter1.addToValues("blue");
    termFilter1.addToExcludes("red");
    AbacusTermFilter termFilter2 = new AbacusTermFilter().setField("category");
    termFilter2.addToValues("sedan");
    AbacusFilter rangeFilter1 = AbacusUtil.buildRangeFilter("year", null, 1999, false, false);
    AbacusFilter rangeFilter2 = AbacusUtil.buildRangeFilter("year", 2000, null, false, false);
    AbacusBooleanFilter booleanFilter1 = new AbacusBooleanFilter();
    booleanFilter1.addToFilters(
        new AbacusBooleanSubFilter().setFilter(rangeFilter1)
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    booleanFilter1.addToFilters(
        new AbacusBooleanSubFilter().setFilter(rangeFilter2)
            .setOccur(AbacusBooleanClauseOccur.SHOULD));

    AbacusBooleanFilter booleanFilter2 = new AbacusBooleanFilter();
    booleanFilter2.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter2))
            .setOccur(AbacusBooleanClauseOccur.MUST));
    booleanFilter2.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setBooleanFilter(booleanFilter1))
            .setOccur(AbacusBooleanClauseOccur.MUST));

    AbacusBooleanFilter booleanFilter = new AbacusBooleanFilter();
    booleanFilter.addToFilters(
        new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setTermFilter(termFilter1))
            .setOccur(AbacusBooleanClauseOccur.SHOULD));
    booleanFilter
        .addToFilters(new AbacusBooleanSubFilter().setFilter(new AbacusFilter().setBooleanFilter(
            booleanFilter2)).setOccur(AbacusBooleanClauseOccur.SHOULD));
    expected.setFilter(new AbacusFilter().setBooleanFilter(booleanFilter));
    expected.setQuery(query);
    assertEquals(expected, request);
  }

  @Test
  public void testNullPred1() throws Exception {
    System.out.println("testNullPred1");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE price IS NOT NULL";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.setFilter(new AbacusFilter().setNullFilter(
        new AbacusNullFilter().setField("price").setFieldType(AbacusFieldType.FLOAT)
            .setReverse(true)));
    assertEquals(expected, request);
  }

  @Test
  public void testNullPred2() throws Exception {
    System.out.println("testNullPred2");
    System.out.println("==================================================");

    String bql = "SELECT * " + "FROM cars " + "WHERE price IS NULL";
    AbacusRequest request = _compiler.compile(bql);
    AbacusRequest expected = new AbacusRequest();
    expected.setFilter(new AbacusFilter().setNullFilter(
        new AbacusNullFilter().setField("price").setFieldType(AbacusFieldType.FLOAT)));
    assertEquals(expected, request);
  }
}
