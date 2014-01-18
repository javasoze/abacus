package abacus.search.codecs;

import java.io.IOException;

import org.apache.lucene.codecs.DocValuesConsumer;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.DocValuesProducer;
import org.apache.lucene.codecs.lucene45.Lucene45DocValuesFormat;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;

public class Abacus46DocValuesFormat extends DocValuesFormat {

  private static final String DV_NAME = "Abacus46DocValuesFormat";
  final DocValuesFormat delegate;
  
  public Abacus46DocValuesFormat() {
    super(DV_NAME);
    this.delegate = new Lucene45DocValuesFormat();
  }
  
  @Override
  public DocValuesConsumer fieldsConsumer(SegmentWriteState state)
      throws IOException {
    return new Abacus46DocValuesConsumer(delegate.fieldsConsumer(state), state);
  }

  @Override
  public DocValuesProducer fieldsProducer(SegmentReadState state)
      throws IOException {
    return new Abacus46DocValuesProducer(delegate.fieldsProducer(state), state);
  }

}
