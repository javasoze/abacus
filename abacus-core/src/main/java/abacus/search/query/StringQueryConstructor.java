package abacus.search.query;

import abacus.api.AbacusBooleanClauseOccur;
import abacus.api.AbacusQuery;
import abacus.api.AbacusSearchField;
import abacus.api.AbacusStringQuery;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StringQueryConstructor extends QueryConstructor {
  @Override
  protected Query construct(AbacusQuery abacusQuery, AbacusQueryParser queryParser)
      throws IOException {
    if (!abacusQuery.isSetStringQuery()) {
      return null;
    }
    AbacusStringQuery stringQuery = abacusQuery.getStringQuery();
    if (!stringQuery.isSetFields()) {
      try {
        return queryParser.parse(stringQuery.getQuery());
      } catch (ParseException e) {
        throw new IOException(e);
      }
    }
    List<Query> queries = new ArrayList<>();
    for (AbacusSearchField field : stringQuery.getFields()) {
      String content = stringQuery.getQuery();
      try {
        if (field.isSetQuery()) {
          content = field.getQuery();
        }
        Query query = queryParser.parse(content);
        query.setBoost((float) field.getBoost());
        queries.add(query);
      } catch (ParseException e) {
        throw new IOException(e);
      }
    }
    if (queries.isEmpty()) {
      return null;
    }

    BooleanClause.Occur booleanClause = BooleanClause.Occur.SHOULD;
    if (stringQuery.getOccur() == AbacusBooleanClauseOccur.MUST) {
      booleanClause = BooleanClause.Occur.MUST;
    }
    BooleanQuery booleanQuery = new BooleanQuery(false);
    for (Query query : queries) {
      booleanQuery.add(query, booleanClause);
    }
    return booleanQuery;
  }
}
