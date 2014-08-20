package abacus.search.filter;

import abacus.api.AbacusBooleanClauseOccur;
import abacus.api.AbacusFilter;
import abacus.api.AbacusTermFilter;
import abacus.service.AbacusQueryParser;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Filter;

public class TermFilterConstructor extends FilterConstructor {
  @Override
  protected Filter construct(AbacusFilter abacusFilter, AbacusQueryParser queryParser) {
    if (!abacusFilter.isSetTermFilter()) {
      return null;
    }
    AbacusTermFilter termFilter = abacusFilter.getTermFilter();
    if (!termFilter.isSetValues() && !termFilter.isSetExcludes()) {
      return null;
    }
    if (!termFilter.isSetExcludes() && termFilter.getValuesSize() == 1) {
      return new TermFilter(new Term(termFilter.getField(), termFilter.getValues().get(0)));
    }
    BooleanFilter filter = new BooleanFilter();
    if (termFilter.isSetValues()) {
      BooleanClause.Occur occur = BooleanClause.Occur.SHOULD;
      if (termFilter.getOccur() == AbacusBooleanClauseOccur.MUST) {
        occur = BooleanClause.Occur.MUST;
      }
      for (int i = 0; i < termFilter.getValuesSize(); ++i) {
        filter.add(new TermFilter(new Term(termFilter.getField(), termFilter.getValues().get(i))),
            occur);
      }
    }
    if (termFilter.isSetExcludes()) {
      for (int i = 0; i < termFilter.getExcludesSize(); ++i) {
        filter.add(new TermFilter(new Term(termFilter.getField(), termFilter.getExcludes().get(i))),
            BooleanClause.Occur.MUST_NOT);
      }
    }
    return filter;
  }
}
