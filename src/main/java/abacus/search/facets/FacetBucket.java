package abacus.search.facets;


public abstract class FacetBucket {

  private final String label;
  
  public FacetBucket(String label) {
    this.label = label;
  }
  
  public String getLabel() {
    return label;
  }

  public abstract int getCount();
  public abstract void accumulate(long val);
}
