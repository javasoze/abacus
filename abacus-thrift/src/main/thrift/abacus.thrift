namespace java abacus.api

enum AbacusBooleanClauseOccur {
  SHOULD = 0,
  MUST,
  MUST_NOT,
}

enum AbacusFieldType {
  STRING = 0,
  INT,
  LONG,
  FLOAT,
  DOUBLE,
}

enum AbacusSortFieldType {
  SCORE = 0,
  STRING,
  INT,
  LONG,
  FLOAT,
  DOUBLE,
}

struct AbacusRange {
  1: required string field
  3: required string startValue
  4: required string endValue
  5: required bool startClosed
  6: required bool endClosed
  7: required AbacusFieldType fieldType
}

struct AbacusRangeQuery {
  1: required AbacusRange range
}

struct AbacusSearchField {
  1: required string name
  2: optional string query
  3: optional double boost = 1
}

struct AbacusStringQuery {
  1: required string query
  2: optional list<AbacusSearchField> fields
  3: optional AbacusBooleanClauseOccur occur = AbacusBooleanClauseOccur.SHOULD
}

struct AbacusTermQuery {
  1: required string field
  2: required string value
}

struct AbacusWildcardQuery {
  1: required string field
  2: required string query
}

// pre-defintion
struct AbacusBooleanQuery {
}

struct AbacusQuery {
  1: optional AbacusRangeQuery rangeQuery
  2: optional AbacusStringQuery stringQuery
  3: optional AbacusTermQuery termQuery
  4: optional AbacusWildcardQuery wildcardQuery
  5: optional AbacusBooleanQuery booleanQuery,
}

struct AbacusBooleanSubQuery {
  1: optional AbacusBooleanClauseOccur occur = AbacusBooleanClauseOccur.SHOULD
  2: optional AbacusQuery query
}

struct AbacusBooleanQuery {
    1: required list<AbacusBooleanSubQuery> queries,
    2: required double minMatch = 1,
    3: required bool disableCoord = 0
}

struct AbacusQueryFilter {
  1: required AbacusQuery query
}

struct AbacusTermFilter {
  1: required string field
  2: optional list<string> values
  3: optional list<string> excludes
  4: optional AbacusBooleanClauseOccur occur = AbacusBooleanClauseOccur.SHOULD
}

struct AbacusRangeFilter {
  1: required AbacusRange range
}

struct AbacusNullFilter {
  1: required string field
  2: required AbacusFieldType fieldType
  3: optional bool reverse = 0
}

// pre-defintion
struct AbacusBooleanFilter {
}

struct AbacusFilter {
  1: optional AbacusBooleanFilter booleanFilter
  2: optional AbacusNullFilter nullFilter
  3: optional AbacusQueryFilter queryFilter
  4: optional AbacusRangeFilter rangeFilter
  5: optional AbacusTermFilter termFilter
}

struct AbacusBooleanSubFilter {
  1: optional AbacusBooleanClauseOccur occur = AbacusBooleanClauseOccur.SHOULD
  2: optional AbacusFilter filter
}

struct AbacusBooleanFilter {
  1: required list<AbacusBooleanSubFilter> filters,
}

struct PagingParam {
  1: optional i32 offset
  2: optional i32 count = 10
}

struct AbacusSortField {
  1: optional string field
  2: required AbacusSortFieldType type
  3: optional bool reverse
}


struct FacetParam {
  1: optional i32 maxNumValues = 5
  2: optional list<string> ranges
}

struct PagingParam {
  1: optional i32 offset
  2: optional i32 count = 10
}

struct Facet {
  1: optional string value
  2: optional i64 count
}

struct AbacusRequest {
  1: optional AbacusQuery query
  2: optional AbacusFilter filter
  3: optional list<AbacusSortField> sortFields
  4: optional map<string, FacetParam> facetParams
  5: optional PagingParam pagingParam
  6: optional bool fetchSrcData
  7: optional bool explain
}

struct AbacusHit {
  1: optional i64 docid
  2: optional double score
  3: optional string explanation
  4: optional string srcData
}

struct AbacusResult {
  1: optional i64 numHits
  2: optional i64 latencyInMs
  3: optional i64 totoalDocs
  4: optional list<AbacusHit> hits
  5: optional map<string, list<Facet>> facetList
}