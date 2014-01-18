package abacus.search.codecs;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.lucene46.Lucene46Codec;

public class Abacus46FacetCodec extends FilterCodec {

  private static final String NAME = "Abacus46Codec";  
  static final Codec defaultCodec = new Lucene46Codec();
  
  private final DocValuesFormat dvFormat = new Abacus46DocValuesFormat();
  
  public Abacus46FacetCodec() {
    super(NAME, defaultCodec);
  }

  @Override
  public DocValuesFormat docValuesFormat() {
    return dvFormat;
  }
  
}
