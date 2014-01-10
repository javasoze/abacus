package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfo.DocValuesType;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;

public class MultiBytesRefFacetAccumulator extends BytesRefFacetAccumulator {

  private SortedSetDocValues currentDocValues = null;
  
  public MultiBytesRefFacetAccumulator(String field) {
    super(field);
  }

  @Override
  public void collect(int doc) throws IOException {   
    currentDocValues.setDocument(doc);
    long ord;
    while ((ord = currentDocValues.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {      
      currentCountInfo.accumulate( (int) ord);
    }
    
  }

  @Override
  protected PerSegmentFacetCount newPerSegmentFacetCount(AtomicReader reader)
      throws IOException {
    FieldInfo finfo = reader.getFieldInfos().fieldInfo(getField());
    if (finfo.getDocValuesType() != DocValuesType.SORTED_SET) {
      throw new IOException("docvalue type expected to be: " + 
          DocValuesType.SORTED_SET +", but was: " + finfo.getDocValuesType());
    }
    
    currentDocValues = reader.getSortedSetDocValues(getField());
    if (currentDocValues == null) {
      throw new IOException("field is not defined: " + getField());
    }
    
    return new PerSegmentFacetCount((int) currentDocValues.getValueCount()) {
      
      SortedSetDocValues docValues = currentDocValues;

      @Override
      public void lookupLabel(int ord, BytesRef result) {
        docValues.lookupOrd(ord, result);
      }     
    };
  }
  
}
