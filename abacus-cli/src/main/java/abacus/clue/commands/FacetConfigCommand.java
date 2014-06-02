package abacus.clue.commands;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;

import abacus.config.FacetsConfig;
import abacus.config.IndexDirectoryFacetsConfigReader;

import com.senseidb.clue.ClueContext;
import com.senseidb.clue.commands.ClueCommand;

public class FacetConfigCommand extends ClueCommand {

	public FacetConfigCommand(ClueContext ctx) {
		super(ctx);
	}

	@Override
	public String getName() {
		return "facetconfig";
	}

	@Override
	public String help() {
		return "Shows facet config";
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
        Map<String, FacetsConfig> facetsConfig = IndexDirectoryFacetsConfigReader.readerFacetsConfig(dirReader);
        for (Entry<String, FacetsConfig> entry : facetsConfig.entrySet()) {
          out.println("name: " + entry.getKey() +", config: " + entry.getValue());
        }
      }
      out.flush();
    } else {
      throw new IllegalArgumentException("can only read facet config from instances of " + DirectoryReader.class);
    }

	}

}
