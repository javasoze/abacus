package abacus.search.facets;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;

public class SortedSetDocValuesOrdReader extends FacetOrdReader {

  private final String field;
  
  public SortedSetDocValuesOrdReader(String field) {
    this.field = field;
  }
  
  @Override
  public FacetOrdSegmentReader getSegmentOrdReader(final AtomicReaderContext ctx)
      throws IOException {
    return new FacetOrdSegmentReader() {
      SortedSetDocValues docVals = ctx.reader().getSortedSetDocValues(field);
      @Override
      public int lookupOrd(BytesRef label) {
        return (int) docVals.lookupTerm(label);
      }
      
      @Override
      public void lookupLabel(int ord, BytesRef label) {
        docVals.lookupOrd(ord, label);
      }
      
      @Override
      public int getValueCount() {
        return (int) docVals.getValueCount();
      }
      
      @Override
      public IntIterator getOrds(int docid) {
        docVals.setDocument(docid);
        long ord;
        IntList ordList = new IntArrayList();
        while ((ord = docVals.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
          if (ord > Integer.MAX_VALUE) {
            throw new IllegalStateException("ord too large: " + ord);
          }
          ordList.add((int)ord);
        }
        return ordList.iterator();
      }
    };
  }

  @Override
  public String getIndexedFieldName() {
    return field;
  }

}
