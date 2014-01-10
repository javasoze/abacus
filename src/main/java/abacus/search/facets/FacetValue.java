package abacus.search.facets;

import java.util.Comparator;

import org.apache.lucene.util.BytesRef;

public class FacetValue {
  protected BytesRef label;
  protected int count;
  
  public static Comparator<FacetValue> COUNT_COMPARATOR = 
      new Comparator<FacetValue> () {

    @Override
    public int compare(FacetValue f1, FacetValue f2) {
      int comp = f2.count - f1.count;
      
      if (comp == 0) {
        return BytesRef.getUTF8SortedAsUnicodeComparator().compare(f1.label, f2.label);
      } else {
        return comp;
      }
    }
    
  };
  
  public FacetValue() {
    this(null, 0);
  }
  
  public FacetValue(BytesRef label, int count) {
    setValues(label, count);
  }
  
  public void setValues(BytesRef label, int count) {
    this.label = label;
    this.count = count;
  }
  
  public BytesRef getLabel() {
    return this.label;
  }
  
  public int getCount() {
    return this.count;
  }
  
  @Override
  public String toString() {
    return label.utf8ToString() + ":" + count;
  }
}
