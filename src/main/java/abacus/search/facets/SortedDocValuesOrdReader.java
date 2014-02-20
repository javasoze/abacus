package abacus.search.facets;

import it.unimi.dsi.fastutil.ints.IntIterator;

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
      public IntIterator getOrds(int docid) {
        return new SingleValueIntIterator(docVals.getOrd(docid));
      }
    };
  }

  @Override
  public String getIndexedFieldName() {
    return field;
  }

}
