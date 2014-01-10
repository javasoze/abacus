package abacus.search.facets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.index.FacetFields;
import org.apache.lucene.facet.params.FacetIndexingParams;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

public class BuildSampleLuceneFacetIndex {

  static void addMetaString(Document doc,CategoryPath[] paths, TaxonomyWriter taxWriter) 
      throws IOException {
    if (paths != null) {      
      FacetFields facetFields = new FacetFields(taxWriter, FacetIndexingParams.DEFAULT);
      facetFields.addFields(doc, Arrays.asList(paths));      
    }
  }
  
  static final String CONTENTS_FIELD = "contents";
  
  static Document buildDoc(JSONObject json, TaxonomyWriter taxWriter) throws Exception{
    Document doc = new Document();
    
    doc.add(new NumericDocValuesField("id", json.getLong("id")));    
    doc.add(new TextField("contents", json.optString("contents"), Store.NO));
    
    CategoryPath[] sval = new CategoryPath[] {
        new CategoryPath("color", json.optString("color")),
        new CategoryPath("year", json.optString("year")),
        new CategoryPath("price", json.optString("price")),
        new CategoryPath("category", json.optString("category")),
        new CategoryPath("mileage", json.optString("mileage")),
    };
    addMetaString(doc,sval, taxWriter);
    
    return doc;
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception{
    if (args.length != 2) {
      System.out.println("usage: source_file index_dir");
    }
    File f = new File(args[0]);
    BufferedReader reader = new BufferedReader(new FileReader(f));
    
    File topDir = new File(args[1]);
    File idxDir = new File(topDir, "index");
    File taxDir = new File(topDir, "tax");
    
    IndexWriterConfig idxWriterConfig = new IndexWriterConfig(Version.LUCENE_44, new StandardAnalyzer(Version.LUCENE_44));
    Directory dir = FSDirectory.open(idxDir);
    IndexWriter writer = new IndexWriter(dir, idxWriterConfig);
    DirectoryTaxonomyWriter taxWriter = new DirectoryTaxonomyWriter(FSDirectory.open(taxDir), OpenMode.CREATE);
    
    
    ArrayList<JSONObject> dataList = new ArrayList<JSONObject>();
    while (true) {
      String line = reader.readLine();
      if (line == null) break;
      
      JSONObject json = new JSONObject(line);
      dataList.add(json);      
    }
    
    int count = 0;
    int max = 1500000;
    while(true) {
      if (count >= max) {
        break;
      }
      JSONObject json = dataList.get(count % dataList.size());
      Document doc = buildDoc(json, taxWriter);
      writer.addDocument(doc);
      count++;
      if (count % 10000 == 0) {
        System.out.print(".");
      }
    }
    
    System.out.println(count+" docs indexed");
    
    reader.close();
    writer.commit();    
    writer.close();
    taxWriter.commit();
    taxWriter.close();
  }

}
