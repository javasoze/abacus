package abacus.search.facets;

import org.apache.lucene.document.FieldType.NumericType;

public class FacetRangeBuilder {

  public static FacetBucket buildFacetRangeBucket(String label, NumericType numericType) {
    switch (numericType) {
    case DOUBLE : return new DoubleFacetRange(label);
    case FLOAT : return new FloatFacetRange(label);
    case INT : return new IntFacetRange(label);
    case LONG: return new LongFacetRange(label);
    default: return new LongFacetRange(label);
    }
  }
  
  private static class DoubleFacetRange extends FacetBucket {

    public DoubleFacetRange(String label) {
      super(label);
    }

    @Override
    public void accumulate(long val) {
      double doubleVal = Double.longBitsToDouble(val);
    }
    
  }
  
  private static class FloatFacetRange extends FacetBucket {

    public FloatFacetRange(String label) {
      super(label);
    }

    @Override
    public void accumulate(long val) {
      float floatVal = Float.intBitsToFloat((int) val);
    }
    
  }
  
  private static class IntFacetRange extends FacetBucket {

    public IntFacetRange(String label) {
      super(label);
    }

    @Override
    public void accumulate(long val) {
      int intVal = (int) val;
    }
    
  }
  
  private static class LongFacetRange extends FacetBucket {

    public LongFacetRange(String label) {
      super(label);
    }

    @Override
    public void accumulate(long val) {
      
    }
    
  }

}
