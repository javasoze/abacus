package org.apache.lucene.util.packed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.lucene.util.packed.PackedInts.Mutable;

class PackedDocSegment {
  int minVal;
  Mutable valSet;

  public long sizeInBytes() {
    return 8 + 4 + valSet.getBitsPerValue()*valSet.size();
  }
  
  static void serialize(PackedDocSegment idset,DataOutputStream out) throws IOException{
    out.writeInt(idset.minVal);
    out.writeInt(idset.valSet.size());
    out.writeInt(idset.valSet.getBitsPerValue());
    int count = idset.valSet.size();
    out.writeInt(count);
    for (int i=0;i<count;++i){
      out.writeLong(idset.valSet.get(i));
    }
  }
  
  static PackedDocSegment deserialize(DataInputStream in, float acceptableOverheadRatio) throws IOException{
    int minVal = in.readInt();
    int valCount = in.readInt();
    int bitsPerVal = in.readInt();
    int len = in.readInt();
    
    Mutable valSet = PackedInts.getMutable(valCount, bitsPerVal, acceptableOverheadRatio);
    
    for (int i=0;i<len;++i){
      valSet.set(i, in.readLong());
    }
    
    PackedDocSegment seg = new PackedDocSegment();
    seg.minVal = minVal;
    seg.valSet = valSet;
    return seg;
  }
}
