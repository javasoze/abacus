package abacus.search.facets;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.IOUtils;

public class FastDocValuesAtomicReader extends FilterAtomicReader {

  private Map<String, NumericDocValues> cached;
  private Map<String, SortedDocValues> sortedCached;  
  private Map<String, SortedSetDocValues> sortedSetCached;  
  
  private List<Closeable> closableList = new ArrayList<Closeable>();
  
  public static enum MemType {
    Heap,
    Direct,
    Native
  };
  
  public FastDocValuesAtomicReader(AtomicReader in, MemType type) throws IOException {
    super(in);
    cached = new HashMap<String, NumericDocValues>();
    sortedCached = new HashMap<String, SortedDocValues>();
    sortedSetCached = new HashMap<String, SortedSetDocValues>();
    for (FieldInfo finfo : in.getFieldInfos()) {
      if (finfo.hasDocValues()) {
        switch(finfo.getDocValuesType()) {
        case NUMERIC: {
          NumericDocValues val;
          if (type == MemType.Heap) {
            val = new ArrayNumericDocValues(super.getNumericDocValues(finfo.name), maxDoc());
          } else if (type == MemType.Direct) {
            val = new DirectBufferNumericDocValues(super.getNumericDocValues(finfo.name), maxDoc());
          } else {
            NativeNumericDocValues nativeVals =
                new NativeNumericDocValues(super.getNumericDocValues(finfo.name), maxDoc());
            closableList.add(nativeVals);
            val = nativeVals;
          }
          cached.put(finfo.name, val);
          break;
        }
        case SORTED : {
          SortedDocValues val;
          if (type == MemType.Heap) {
            val = new ArraySortedDocValues(super.getSortedDocValues(finfo.name), maxDoc());
          } else if (type == MemType.Direct) {
            val = new DirectBufferSortedDocValues(super.getSortedDocValues(finfo.name), maxDoc());
          } else {
            NativeSortedDocValues nativeVals =
                new NativeSortedDocValues(super.getSortedDocValues(finfo.name), maxDoc());
            closableList.add(nativeVals);
            val = nativeVals;
          }
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

  @Override
  protected void doClose() throws IOException {
    IOUtils.close(closableList);
  }
}
