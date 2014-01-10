package abacus.search.facets;

public interface FacetEntryIterator {
  boolean next(FacetValue val);
  
  public static FacetEntryIterator EMPTY = new FacetEntryIterator() {
    @Override
    public boolean next(FacetValue val) {
      return false;
    }
    
  };
}
