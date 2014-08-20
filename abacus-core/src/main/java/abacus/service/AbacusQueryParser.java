package abacus.service;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

public interface AbacusQueryParser {
  Query parse(String rawQuery) throws ParseException;

  public static class DefaultQueryParser implements AbacusQueryParser {
    private final org.apache.lucene.queryparser.classic.QueryParser parser;

    public DefaultQueryParser(String defaultField, Analyzer analyzer) {
      parser = new org.apache.lucene.queryparser.classic.QueryParser(Version.LUCENE_48,
          defaultField, analyzer);
    }

    @Override
    public Query parse(String rawQuery) throws ParseException {
      return parser.parse(rawQuery);
    }
  }
}
