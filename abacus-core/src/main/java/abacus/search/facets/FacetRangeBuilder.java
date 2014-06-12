package abacus.search.facets;

import java.text.ParseException;

import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.NumericRangeFilter;

public class FacetRangeBuilder {

  public static FacetRange buildFacetRangeBucket(String label, NumericType numericType) 
      throws ParseException {
    if (numericType == null) {
      return new FacetRange(label);
    }
    switch (numericType) {
    case DOUBLE : return new DoubleFacetRange(label);
    case FLOAT : return new FloatFacetRange(label);
    case INT : return new IntFacetRange(label);
    case LONG: return new LongFacetRange(label);
    default: return new LongFacetRange(label);
    }
  }  
  
  public static class FacetRange extends FacetBucket {

    protected final boolean incLower;
    protected final boolean incUpper;
    protected final String lower;
    protected final String upper;
    
    public FacetRange(String rangeString) throws ParseException {
      super(rangeString);
      
      int index2 = rangeString.indexOf(" TO ");
      if (index2 == -1) {
        throw new ParseException("cannot parse: " + rangeString, -1);
      }
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
    
    public Filter buildRangeFilter(String field) {
      return null;
    }

    @Override
    public void accumulate(long val) {
      // TODO Auto-generated method stub
      
    }
    
  }
  
  private static class DoubleFacetRange extends FacetRange {

    private final double lowerVal;
    private final double upperVal;
    
    public DoubleFacetRange(String label) throws ParseException {
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

    @Override
    public Filter buildRangeFilter(String field) {
      return NumericRangeFilter.newDoubleRange(field, lowerVal, upperVal, incLower, incUpper);
    }
    
  }
  
  private static class FloatFacetRange extends FacetRange {
    private final float lowerVal;
    private final float upperVal;
    public FloatFacetRange(String label) throws ParseException {
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
    
    @Override
    public Filter buildRangeFilter(String field) {
      return NumericRangeFilter.newFloatRange(field, lowerVal, upperVal, incLower, incUpper);
    }
    
  }
  
  private static class IntFacetRange extends FacetRange {
    private final int lowerVal;
    private final int upperVal;
    
    public IntFacetRange(String label) throws ParseException {
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
   
    @Override
    public Filter buildRangeFilter(String field) {
      return NumericRangeFilter.newIntRange(field, lowerVal, upperVal, incLower, incUpper);
    }
  }
  
  private static class LongFacetRange extends FacetRange {
    private final long lowerVal;
    private final long upperVal;
    public LongFacetRange(String label) throws ParseException {
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
    
    @Override
    public Filter buildRangeFilter(String field) {
      return NumericRangeFilter.newLongRange(field, lowerVal, upperVal, incLower, incUpper);
    }
    
  }

}
