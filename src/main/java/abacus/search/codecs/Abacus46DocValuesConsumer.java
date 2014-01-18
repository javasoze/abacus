package abacus.search.codecs;

import java.io.IOException;

import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.util.BytesRef;

public class Abacus46DocValuesConsumer extends DocValuesConsumer {

  private final DocValuesConsumer delegate;
  Abacus46DocValuesConsumer(DocValuesConsumer delegate, SegmentWriteState state) throws IOException {
    this.delegate = delegate;
  }
  
  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public void addNumericField(FieldInfo field, Iterable<Number> values)
      throws IOException {
    delegate.addNumericField(field, values);
  }

  @Override
  public void addBinaryField(FieldInfo field, Iterable<BytesRef> values)
      throws IOException {
    delegate.addBinaryField(field, values);
  }

  @Override
  public void addSortedField(FieldInfo field, Iterable<BytesRef> values,
      Iterable<Number> docToOrd) throws IOException {
    delegate.addSortedField(field, values, docToOrd);
  }

  @Override
  public void addSortedSetField(FieldInfo field, Iterable<BytesRef> values,
      Iterable<Number> docToOrdCount, Iterable<Number> ords) throws IOException {
    delegate.addSortedSetField(field, values, docToOrdCount, ords);
  }

}
