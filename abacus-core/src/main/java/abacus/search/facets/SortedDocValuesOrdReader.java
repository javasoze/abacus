package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;

public class SortedDocValuesOrdReader extends FacetOrdReader {

  private final String field;
  
  public SortedDocValuesOrdReader(String field) {
    this.field = field;
  }
  
  @Override
  public FacetOrdSegmentReader getSegmentOrdReader(final AtomicReaderContext ctx) 
      throws IOException {
    
    return new FacetOrdSegmentReader() {
      SortedDocValues docVals = ctx.reader().getSortedDocValues(field);
      int ord = -1;
      @Override
      public int lookupOrd(BytesRef label) {
        return docVals.lookupTerm(label);
      }
      
      @Override
      public void lookupLabel(int ord, BytesRef label) {
        docVals.lookupOrd(ord, label);
      }
      
      @Override
      public int getValueCount() {
        return docVals.getValueCount();
      }
      
      @Override
      public void setDocument(int docid) {
        ord = docVals.getOrd(docid);
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
