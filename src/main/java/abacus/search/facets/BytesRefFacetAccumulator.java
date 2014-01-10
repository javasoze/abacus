package abacus.search.facets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.facet.collections.IntArray;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfo.DocValuesType;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefHash;

public class BytesRefFacetAccumulator extends FacetAccumulator {

  protected PerSegmentFacetCount currentCountInfo = null;
  private SortedDocValues currentDocValues = null;
  private final List<PerSegmentFacetCount> countInfoList = new ArrayList<PerSegmentFacetCount>();
  
  public BytesRefFacetAccumulator(String field) {
    super(field);
  }

  @Override
  public void collect(int doc) throws IOException {
    currentCountInfo.accumulate(currentDocValues.getOrd(doc));
  }
  
  protected PerSegmentFacetCount newPerSegmentFacetCount(AtomicReader reader) throws IOException {
    FieldInfo finfo = reader.getFieldInfos().fieldInfo(getField());
    if (finfo.getDocValuesType() != DocValuesType.SORTED) {
      throw new IOException("docvalue type expected to be: " + 
          DocValuesType.SORTED +", but was: " + finfo.getDocValuesType());
    }
    
    currentDocValues = reader.getSortedDocValues(getField());
    if (currentDocValues == null) {
      throw new IOException("field is not defined: " + getField());
    }
    
    return new PerSegmentFacetCount(currentDocValues.getValueCount()) {
      
      SortedDocValues docValues = currentDocValues;

      @Override
      public void lookupLabel(int ord, BytesRef result) {
        docValues.lookupOrd(ord, result);
      }
      
    };
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    currentCountInfo = newPerSegmentFacetCount(context.reader());
    countInfoList.add(currentCountInfo);
  }
  
  @Override
  public FacetEntryIterator getFacetEntryIterator(final int minHit) {
    if (countInfoList.size() == 1) {
      return currentCountInfo.getFacetEntryIterator(minHit);
    } else if (countInfoList.size() == 0){
      return FacetEntryIterator.EMPTY;
    } else {
      final BytesRefHash hash = new BytesRefHash();
      final IntArray countList = new IntArray();      
      for (PerSegmentFacetCount countInfo : countInfoList) {
        FacetEntryIterator iter = countInfo.getFacetEntryIterator(1);
        
        while(true) {
          FacetValue value = new FacetValue();
          if (!iter.next(value)) {
            break;
          }
          int id;
          if ((id = hash.find(value.label)) < 0) {
            // not found
            hash.add(value.label);
            countList.addToArray(value.count);
          } else {
            int count = countList.get(id) + 1;
            countList.set(id, count);
          }
        }
      }
      
      for (int i =0;i < hash.size(); ++i) {
        BytesRef bref = new BytesRef();
        hash.get(i, bref);
      }
      
      return new FacetEntryIterator() {
        int numElems = hash.size();
        int idx = -1;
        @Override
        public boolean next(FacetValue val) {
          while(true) {
            idx++;
            if (idx < numElems) {
              int count = countList.get(idx);
              if (count < minHit) {
                continue;
              }
              if (val.label == null) {
                val.label = new BytesRef();
              }
              hash.get(idx, val.label);
              val.count = count;
              return true;
            }
            return false;
          }
        }
      };
    }
  }

}
