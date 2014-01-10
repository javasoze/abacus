package abacus.search.facets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;

public class FastDocValuesAtomicReader extends FilterAtomicReader {

  private Map<String, NumericDocValues> cached;
  private Map<String, SortedDocValues> sortedCached;  
  private Map<String, SortedSetDocValues> sortedSetCached;  
  public FastDocValuesAtomicReader(AtomicReader in, boolean useDirect) throws IOException {
    super(in);
    cached = new HashMap<String, NumericDocValues>();
    sortedCached = new HashMap<String, SortedDocValues>();
    sortedSetCached = new HashMap<String, SortedSetDocValues>();
    for (FieldInfo finfo : in.getFieldInfos()) {
      if (finfo.hasDocValues()) {
        switch(finfo.getDocValuesType()) {
        case NUMERIC: {
          NumericDocValues val = useDirect ? 
              new DirectBufferNumericDocValues(super.getNumericDocValues(finfo.name), maxDoc()) :
              new ArrayNumericDocValues(super.getNumericDocValues(finfo.name), maxDoc());
          cached.put(finfo.name, val);
          break;
        }
        case SORTED : {
          SortedDocValues val = useDirect ?
              new DirectBufferSortedDocValues(super.getSortedDocValues(finfo.name), maxDoc()) :
              new ArraySortedDocValues(super.getSortedDocValues(finfo.name), maxDoc());
          sortedCached.put(finfo.name, val);
          break;
        }
        case SORTED_SET : {
          SortedSetDocValues val =
              new ArraySortedSetDocValues(super.getSortedSetDocValues(finfo.name), maxDoc());
          sortedSetCached.put(finfo.name, val);
          break;
        }
        default : {
          break;
        }
        }
      }
    }
  }

  @Override
  public NumericDocValues getNumericDocValues(String field) throws IOException {
    return cached.get(field);
  }

  @Override
  public SortedDocValues getSortedDocValues(String field) throws IOException {
    return sortedCached.get(field);
  }
  
  
}
