package abacus.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

public class IndexDirectoryFacetsConfigReader implements FieldConfigReader {

  private final Directory idxDir;
  
  public IndexDirectoryFacetsConfigReader(Directory idxDir) {
    this.idxDir = idxDir;
  }
  
  public static Map<String, FieldConfig> readerFacetsConfig(DirectoryReader reader)
      throws IOException {
    IndexCommit idxCommit = reader.getIndexCommit();
    Map<String, String> commitData = idxCommit.getUserData();
    return FieldConfig.deFlatten(commitData);
  }
  
  @Override
  public Map<String, FieldConfig> readerFacetsConfig() throws IOException {
    DirectoryReader reader = DirectoryReader.open(idxDir);
    Map<String, FieldConfig> configMap = readerFacetsConfig(reader);
    reader.close();
    return configMap;
  }
  
  public static void putFacetsConfig(IndexWriter idxWriter, Map<String, FieldConfig> configMap)
      throws IOException {
    Map<String, String> commitData = new HashMap<String, String>();
    Map<String, String> srcCommit = idxWriter.getCommitData();
    if (srcCommit != null) {
      commitData.putAll(srcCommit);
    }
    commitData.putAll(FieldConfig.flatten(configMap));
    idxWriter.setCommitData(commitData);
  }

}
