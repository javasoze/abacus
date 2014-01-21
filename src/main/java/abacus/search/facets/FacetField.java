package abacus.search.facets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.DocValuesType;
import org.apache.lucene.util.BytesRef;

public class FacetField extends Field {

  public static final String FIELD_NAME = "$.FACET";
  private static FieldType TYPE = new FieldType();
  
  public static final char DEFAULT_FACET_DELIM_CHAR = '\u001F';
  
  static {
    TYPE.setDocValueType(DocValuesType.SORTED_SET);
    TYPE.freeze();
  }
  
  protected FacetField(String name, BytesRef value) {
    super(FIELD_NAME, TYPE);
    BytesRef nameBytes = new BytesRef(name);
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    bout.write(nameBytes.bytes, nameBytes.offset, nameBytes.length);
    bout.write(DEFAULT_FACET_DELIM_CHAR);
    bout.write(value.bytes, value.offset, value.length);
    try {
      bout.flush();
      bout.close();
    } catch (IOException e) {
      // ignore
    }
    
    byte[] resultBytes = bout.toByteArray();
    super.fieldsData = new BytesRef(resultBytes);    
  }  
}
