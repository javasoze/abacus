package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;

public class SortedDocValuesOrdReader extends FacetOrdReader {

  private final String field;
  
  public SortedDocValuesOrdReader(String field) {
    this.field = field;
  }
  
  @Override
  public FacetOrdSegmentReader getSegmentOrdReader(final LeafReaderContext ctx) 
      throws IOException {
    
    return new FacetOrdSegmentReader() {
      SortedDocValues docVals = ctx.reader().getSortedDocValues(field);
      int ord = -1;
      @Override
      public int lookupOrd(BytesRef label) {
        return docVals.lookupTerm(label);
      }
      
      @Override
      public BytesRef lookupLabel(int ord) {
        return docVals.lookupOrd(ord);
      }
      
      @Override
      public int getValueCount() {
        return docVals.getValueCount();
      }
      
      @Override
      public void setDocument(int docId) {
        ord = docVals.getOrd(docId);
      }

      @Override
      public long nextOrd() {
        int retOrd = ord;
        ord = -1;
        return retOrd;
      }
    };
  }

  @Override
  public String getIndexedFieldName() {
    return field;
  }

}
