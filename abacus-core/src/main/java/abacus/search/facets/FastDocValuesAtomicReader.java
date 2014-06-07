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

import abacus.search.facets.docvalues.ArrayNumericDocValues;
import abacus.search.facets.docvalues.ArraySortedDocValues;
import abacus.search.facets.docvalues.ArraySortedSetDocValues;
import abacus.search.facets.docvalues.DirectBufferNumericDocValues;
import abacus.search.facets.docvalues.DirectBufferSortedDocValues;
import abacus.search.facets.docvalues.DirectBufferSortedSetDocValues;
import abacus.search.facets.docvalues.NativeNumericDocValues;
import abacus.search.facets.docvalues.NativeSortedDocValues;
import abacus.search.facets.docvalues.NativeSortedSetDocValues;

public class FastDocValuesAtomicReader extends FilterAtomicReader {

  private Map<String, NumericDocValues> cached;
  private Map<String, SortedDocValues> sortedCached;  
  private Map<String, SortedSetDocValues> sortedSetCached;  
  
  private List<Closeable> closableList = new ArrayList<Closeable>();
  
  public static enum MemType {
    Default,
    Heap,
    Direct,
    Native
  };
  
  public FastDocValuesAtomicReader(AtomicReader in, Map<String, MemType> loadOptionsMap) 
      throws IOException {
    this(in, loadOptionsMap, MemType.Default);    
  }
  
  public FastDocValuesAtomicReader(AtomicReader in, Map<String, MemType> loadOptionsMap, MemType defaultMemType) 
      throws IOException {
    super(in);
    cached = new HashMap<String, NumericDocValues>();
    sortedCached = new HashMap<String, SortedDocValues>();
    sortedSetCached = new HashMap<String, SortedSetDocValues>();
    for (FieldInfo finfo : in.getFieldInfos()) {
      MemType type = loadOptionsMap != null ? loadOptionsMap.get(finfo.name) : null;
      if (type == null) {
        type = MemType.Default;
      }
      if (finfo.hasDocValues()) {
        switch(finfo.getDocValuesType()) {
        case NUMERIC: {
          NumericDocValues val = super.getNumericDocValues(finfo.name);
          if (type == MemType.Heap) {
            val = new ArrayNumericDocValues(val, maxDoc());
          } else if (type == MemType.Direct) {
            val = new DirectBufferNumericDocValues(val, maxDoc());
          } else if (type == MemType.Native){
            NativeNumericDocValues nativeVals =
                new NativeNumericDocValues(val, maxDoc());
            closableList.add(nativeVals);
            val = nativeVals;
          }
          cached.put(finfo.name, val);
          break;
        }
        case SORTED : {
          SortedDocValues val = super.getSortedDocValues(finfo.name);
          if (type == MemType.Heap) {
            val = new ArraySortedDocValues(val, maxDoc());
          } else if (type == MemType.Direct) {
            val = new DirectBufferSortedDocValues(val, maxDoc());
          } else if (type == MemType.Native){
            NativeSortedDocValues nativeVals =
                new NativeSortedDocValues(val, maxDoc());
            closableList.add(nativeVals);
            val = nativeVals;
          }
          sortedCached.put(finfo.name, val);
          break;
        }
        case SORTED_SET : {
          SortedSetDocValues val = super.getSortedSetDocValues(finfo.name);
          if (type == MemType.Heap) {
            val = new ArraySortedSetDocValues(val, maxDoc());
          } else if (type == MemType.Direct) {
            val = new DirectBufferSortedSetDocValues(val, maxDoc());
          } else if (type == MemType.Native){
            NativeSortedSetDocValues nativeVals =
                new NativeSortedSetDocValues(val, maxDoc());
            closableList.add(nativeVals);
            val = nativeVals;
          }
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
