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
  
  private static abstract class FacetRange extends FacetBucket {

    protected final boolean incLower;
    protected final boolean incUpper;
    protected final String lower;
    protected final String upper;
    
    public FacetRange(String rangeString) {
      super(rangeString);
      
      int index2 = rangeString.indexOf(" TO ");
      boolean incLower = true, incUpper = true;

      if (rangeString.trim().startsWith("(")) {
        incLower = false;
      }

      if (rangeString.trim().endsWith(")")) {
        incUpper = false;
      }

      int index = -1, index3 = -1;

      if (incLower == true) {
        index = rangeString.indexOf('[');
      } else if (incLower == false) {
        index = rangeString.indexOf('(');
      }

      if (incUpper == true) {
        index3 = rangeString.indexOf(']');
      } else if (incUpper == false) {
        index3 = rangeString.indexOf(')');
      }
      lower = rangeString.substring(index + 1, index2).trim();
      upper = rangeString.substring(index2 + 4, index3).trim();
      this.incLower = incLower;
      this.incUpper = incUpper;
    }    
  }
  
  private static class DoubleFacetRange extends FacetRange {

    private final double lowerVal;
    private final double upperVal;
    public DoubleFacetRange(String label) {
      super(label);
      lowerVal = "*".equals(lower) ? Double.MIN_VALUE : Double.parseDouble(lower);
      upperVal = "*".equals(upper) ? Double.MAX_VALUE : Double.parseDouble(upper);
    }

    @Override
    public void accumulate(long longVal) {
      double val = Double.longBitsToDouble(longVal);
      if (val < upperVal && val > lowerVal) {
        count++;
      } else if (incLower && val == lowerVal) {
        count++;
      } else if (incUpper && val == upperVal) {
        count++;
      }
    }
    
  }
  
  private static class FloatFacetRange extends FacetRange {
    private final float lowerVal;
    private final float upperVal;
    public FloatFacetRange(String label) {
      super(label);
      lowerVal = "*".equals(lower) ? Float.MIN_VALUE : Float.parseFloat(lower);
      upperVal = "*".equals(upper) ? Float.MAX_VALUE : Float.parseFloat(upper);
    }

    @Override
    public void accumulate(long longVal) {
      float val = Float.intBitsToFloat((int) longVal);
      if (val < upperVal && val > lowerVal) {
        count++;
      } else if (incLower && val == lowerVal) {
        count++;
      } else if (incUpper && val == upperVal) {
        count++;
      }
    }
    
  }
  
  private static class IntFacetRange extends FacetRange {
    private final float lowerVal;
    private final float upperVal;
    
    public IntFacetRange(String label) {
      super(label);
      lowerVal = "*".equals(lower) ? Integer.MIN_VALUE : Integer.parseInt(lower);
      upperVal = "*".equals(upper) ? Integer.MAX_VALUE : Integer.parseInt(upper);
    }

    @Override
    public void accumulate(long longVal) {
      int val = (int) longVal;
      if (val < upperVal && val > lowerVal) {
        count++;
      } else if (incLower && val == lowerVal) {
        count++;
      } else if (incUpper && val == upperVal) {
        count++;
      }
    }
    
  }
  
  private static class LongFacetRange extends FacetRange {
    private final long lowerVal;
    private final long upperVal;
    public LongFacetRange(String label) {
      super(label);
      lowerVal = "*".equals(lower) ? Long.MIN_VALUE : Long.parseLong(lower);
      upperVal = "*".equals(upper) ? Long.MIN_VALUE : Long.parseLong(upper);      
    }

    @Override
    public void accumulate(long val) {
      if (val < upperVal && val > lowerVal) {
        count++;
      } else if (incLower && val == lowerVal) {
        count++;
      } else if (incUpper && val == upperVal) {
        count++;
      }
    }
    
  }

}
