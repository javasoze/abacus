package abacus.search.facets.docvalues;

public class SortedSetDocValuesUtil {
  public static final int UNASSIGNED = Integer.MAX_VALUE;
  public static final long HIGHEST_BIT = 1L << 31;
  public static final long HIGHEST_BIT_INVERSE = HIGHEST_BIT - 1;
  
  public static int setHighestBit(int val) {
    return (int) ((long) val | HIGHEST_BIT);
  }
  
  public static boolean isSetHighestBit(int val) {
    return ((long) val & HIGHEST_BIT) != 0;
  }
  
  public static int decodePointer(int val) {
    return (int) ((long) val & HIGHEST_BIT_INVERSE);
  }
}
