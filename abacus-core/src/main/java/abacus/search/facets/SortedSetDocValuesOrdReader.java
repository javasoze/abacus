package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;

public class SortedSetDocValuesOrdReader extends FacetOrdReader {

  private final String field;
  
  public SortedSetDocValuesOrdReader(String field) {
    this.field = field;
  }
  
  @Override
  public FacetOrdSegmentReader getSegmentOrdReader(final LeafReaderContext ctx)
      throws IOException {
    return new FacetOrdSegmentReader() {
      SortedSetDocValues docVals = ctx.reader().getSortedSetDocValues(field);
      @Override
      public int lookupOrd(BytesRef label) {
        return (int) docVals.lookupTerm(label);
      }
      
      @Override
      public BytesRef lookupLabel(int ord) {        
        return docVals.lookupOrd(ord);
      }
      
      @Override
      public int getValueCount() {
        return (int) docVals.getValueCount();
      }      

      @Override
      public void setDocument(int docid) {
        docVals.setDocument(docid);
      }

      @Override
      public long nextOrd() {
        return docVals.nextOrd();
      }
    };
  }

  @Override
  public String getIndexedFieldName() {
    return field;
  }

}
