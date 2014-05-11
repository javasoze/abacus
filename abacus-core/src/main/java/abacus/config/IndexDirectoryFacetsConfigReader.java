package abacus.config;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

public class IndexDirectoryFacetsConfigReader implements FacetsConfigReader {

  private final Directory idxDir;
  
  public IndexDirectoryFacetsConfigReader(Directory idxDir) {
    this.idxDir = idxDir;
  }
  
  public static Map<String, FacetsConfig> readerFacetsConfig(DirectoryReader reader) 
      throws IOException {
    IndexCommit idxCommit = reader.getIndexCommit();
    Map<String, String> commitData = idxCommit.getUserData();
    return FacetsConfig.deFlatten(commitData);
  }
  
  @Override
  public Map<String, FacetsConfig> readerFacetsConfig() throws IOException {
    DirectoryReader reader = DirectoryReader.open(idxDir);
    Map<String, FacetsConfig> configMap = readerFacetsConfig(reader);
    reader.close();
    return configMap;
  }
  
  public static void putFacetsConfig(IndexWriter idxWriter, Map<String, FacetsConfig> configMap) 
      throws IOException {    
    Map<String, String> commitData = idxWriter.getCommitData();
    commitData.putAll(FacetsConfig.flatten(configMap));
    idxWriter.setCommitData(commitData);
  }

}
