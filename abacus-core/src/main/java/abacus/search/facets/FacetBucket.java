package abacus.search.facets;


public abstract class FacetBucket {

  private final String label;
  protected int count = 0;
  
  public FacetBucket(String label) {
    this.label = label;
  }
  
  public String getLabel() {
    return label;
  }

  public int getCount() {
    return count;
  }
  
  public abstract void accumulate(long val);
}
