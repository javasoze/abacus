package abacus.search.codecs;

import java.io.IOException;

import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.Bits;

public class Abacus46DocValuesProducer extends DocValuesProducer {

  private final DocValuesProducer delegate;
  
  Abacus46DocValuesProducer(DocValuesProducer delegate, SegmentReadState state) throws IOException {
    this.delegate = delegate;
  }
  
  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public NumericDocValues getNumeric(FieldInfo field) throws IOException {
    return delegate.getNumeric(field);
  }

  @Override
  public BinaryDocValues getBinary(FieldInfo field) throws IOException {
    return delegate.getBinary(field);
  }

  @Override
  public SortedDocValues getSorted(FieldInfo field) throws IOException {
    return delegate.getSorted(field);
  }

  @Override
  public SortedSetDocValues getSortedSet(FieldInfo field) throws IOException {
    return delegate.getSortedSet(field);
  }

  @Override
  public Bits getDocsWithField(FieldInfo field) throws IOException {
    return delegate.getDocsWithField(field);
  }

  @Override
  public long ramBytesUsed() {
    return delegate.ramBytesUsed();
  }

}
