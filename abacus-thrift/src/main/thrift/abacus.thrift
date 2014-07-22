namespace java abacus.api

enum AbacusBooleanClauseOccur {
  SHOULD = 0,
  MUST,
  MUST_NOT,
}

struct AbacusStringQuery {
  1: required string query
  2: optional list<string> fields
  3: optional AbacusBooleanClauseOccur occur = AbacusBooleanClauseOccur.SHOULD
}

struct AbacusWildcardQuery {
  1: required string query
  2: required string field
}

struct AbacusQuery {
  1: optional AbacusStringQuery stringQuery
  2: optional AbacusWildcardQuery wildcardQuery
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
  1: required string field
  3: required string startValue
  4: required string endValue
  5: required bool startClosed
  6: required bool endClosed
}

struct AbacusNullFilter {
  1: required string field
}

// pre-defintion
struct AbacusBooleanFilter {
}

struct AbacusFilter {
  1: optional AbacusQueryFilter queryFilter
  2: optional AbacusTermFilter termFilter
  3: optional AbacusRangeFilter rangeFilter
  4: optional AbacusNullFilter nullFilter
  5: optional AbacusBooleanFilter booleanFilter
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

enum SortMode {
  SCORE = 0,
  CUSTOM = 1
}

struct SortField {
  1: optional SortMode mode = SortMode.SCORE
  2: optional string field
  3: optional bool reverse
}

enum FacetSortMode {
  HITS_DESC = 0,
  VALUE_ASC = 1
}

struct FacetParam {
  1: optional FacetSortMode mode = FacetSortMode.HITS_DESC
  2: optional i32 maxNumValues = 5
  3: optional i32 minCount = 1
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
  3: optional list<SortField> sortFields
  4: optional map<string, FacetParam> facetParams
  5: optional PagingParam pagingParam
  6: optional bool fetchSrcData
  7: optional bool explain
}

struct AbacusHit {
  1: optional i64 docid
  2: optional double score
  3: optional map<string, list<string>> fields
  4: optional string explanation
}

struct AbacusResult {
  1: optional i64 numHits
  2: optional i64 latencyInMs
  3: optional i64 totoalDocs
  4: optional list<AbacusHit> hits
  5: optional map<string, list<Facet>> facetList
}