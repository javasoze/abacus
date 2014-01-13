package org.apache.lucene.util.packed;

import java.io.IOException;

import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.packed.PackedInts.Format;
import org.apache.lucene.util.packed.PackedInts.Mutable;
import org.apache.lucene.util.packed.PackedInts.Reader;

class PackedDocSegment {
  int minVal;
  Mutable valSet;

  public long ramBytesUsed() {
    return RamUsageEstimator.alignObjectSize(RamUsageEstimator.NUM_BYTES_OBJECT_REF) 
        + RamUsageEstimator.NUM_BYTES_INT + valSet.ramBytesUsed();
  }
  
  static void serialize(PackedDocSegment segment, DataOutput out) throws IOException{
    out.writeVInt(segment.minVal);
    segment.valSet.save(out);
  }
  
  static PackedDocSegment deserialize(DataInput in) throws IOException{
    int minVal = in.readVInt();    
    
    Reader reader = PackedInts.getReader(in);
    
    int bitsPerVal = reader.getBitsPerValue();
    int valCount = reader.size();
    
    
    Mutable valSet = PackedInts.getMutable(valCount, bitsPerVal, Format.PACKED);
    
    PackedDocSegment seg = new PackedDocSegment();
    seg.minVal = minVal;
    seg.valSet = valSet;
    return seg;
  }
}
