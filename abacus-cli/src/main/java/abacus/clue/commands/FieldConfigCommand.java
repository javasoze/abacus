package abacus.clue.commands;

import abacus.config.FieldConfig;
import abacus.config.IndexDirectoryFacetsConfigReader;
import com.senseidb.clue.ClueContext;
import com.senseidb.clue.commands.ClueCommand;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

public class FieldConfigCommand extends ClueCommand {

  public FieldConfigCommand(ClueContext ctx) {
    super(ctx);
  }

  @Override
  public String getName() {
    return "fieldconfig";
  }

  @Override
  public String help() {
    return "Shows field config";
  }

  @Override
  public void execute(String[] args, PrintStream out) throws Exception {
    IndexReader reader = ctx.getIndexReader();
    if (reader instanceof DirectoryReader) {
      DirectoryReader dirReader = (DirectoryReader) reader;
      Map<String, String> userData = dirReader.getIndexCommit().getUserData();
      if (userData == null || userData.size() == 0) {
        out.println("No facet config found");
      } else {
        Map<String, FieldConfig> facetsConfig = IndexDirectoryFacetsConfigReader
            .readerFacetsConfig(dirReader);
        for (Entry<String, FieldConfig> entry : facetsConfig.entrySet()) {
          out.println("filed: " + entry.getKey() + ", config: " + entry.getValue());
        }
      }
      out.flush();
    } else {
      throw new IllegalArgumentException(
          "can only read facet config from instances of " + DirectoryReader.class);
    }

  }

}
