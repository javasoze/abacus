package abacus.search.facets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FacetLabel;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

public class IndexGenerator {

  static FacetsConfig FACETS_CONFIG = new FacetsConfig();
  static {
    FACETS_CONFIG.setMultiValued("tags", true);
  }  
  
  static void addMetaString(Document doc, String field, String value) {
    if (value != null) {
      doc.add(new SortedDocValuesField(field, new BytesRef(value)));
      doc.add(new StringField(field+"_indexed", value, Store.YES));
      doc.add(buildFacetsField(field, value));
    }
  }
  
  static final String CONTENTS_FIELD = "contents";
  
  static Field buildFacetsField(String name, String label) {
    FacetLabel cp = new FacetLabel(name, label);
    String fullPath = FacetsConfig.pathToString(cp.components, cp.length);
    //System.out.println("add " + fullPath);

    // For facet counts:
    return new SortedSetDocValuesField(FacetsConfig.DEFAULT_INDEX_FIELD_NAME, new BytesRef(fullPath));
  }
  
  static Document buildDoc(JSONObject json) throws Exception{
    Document doc = new Document();
    
    // uid
    doc.add(new NumericDocValuesField("id", json.getLong("id")));
    
    // contents text field
    doc.add(new TextField("contents", json.optString("contents"), Store.NO));
    
    // range fields
    doc.add(new NumericDocValuesField("price", json.optLong("price")));
    
    int year = json.optInt("year");
    doc.add(new NumericDocValuesField("year", year));
    doc.add(buildFacetsField("year", String.valueOf(year)));
    
    int miles = json.optInt("mileage");
    doc.add(new NumericDocValuesField("mileage", miles));
    doc.add(buildFacetsField("mileage", String.valueOf(miles)));
    
    
    addMetaString(doc,"color", json.optString("color"));
    
    addMetaString(doc,"category", json.optString("category"));
    
    String tagsString = json.optString("tags");
    if (tagsString != null) {
      String[] parts = tagsString.split(",");
      if (parts != null && parts.length > 0) {
        for (String part : parts) {
          doc.add(new SortedSetDocValuesField("tags", new BytesRef(part)));
          doc.add(new StringField("tags_indexed", part, Store.NO));
          doc.add(buildFacetsField("tags", part));
        }
      }      
    }        
    FACETS_CONFIG.build(doc);    
    return doc;
  }
  
  static Directory buildSmallIndex(File dataFile) throws Exception {
    BufferedReader reader = new BufferedReader(new FileReader(dataFile));
    
    
    IndexWriterConfig idxWriterConfig = new IndexWriterConfig(Version.LUCENE_47, new StandardAnalyzer(Version.LUCENE_47));
    Directory dir = new RAMDirectory();
    IndexWriter writer = new IndexWriter(dir, idxWriterConfig);
    int count = 0;
    while (true) {
      String line = reader.readLine();
      if (line == null) break;
      
      JSONObject json = new JSONObject(line);
      Document doc = buildDoc(json);
      writer.addDocument(doc);
      count++;      
    }
    
    System.out.println(count+" seed docs indexed");
    
    reader.close();
    writer.commit();
    writer.close();
    
    return dir;
  }
  
  static void buildLargeIndex(Directory smallIndex, int numChunks, File outDir) throws Exception{
    IndexWriterConfig idxWriterConfig = new IndexWriterConfig(Version.LUCENE_47, null);
    Directory tempDir = FSDirectory.open(outDir);
    IndexWriter writer = new IndexWriter(tempDir, idxWriterConfig);
    // index size = 15000 * numChunks
    IndexReader reader = DirectoryReader.open(smallIndex);
    for (int i = 0; i < numChunks; ++i) {
      writer.addIndexes(reader);
      System.out.println((i+1)*reader.maxDoc() +" docs indexed.");
    }
    
    System.out.println("merging all segments");
    writer.forceMerge(1);
    
    writer.commit();
    writer.close();  
    reader.close();
  }
  
  
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.out.println("usage: source_file index_dir");
    }
    
    File f = new File(args[0]);
    File outDir = new File(args[1]);
    
    int numChunks = 1;    
    try {
      numChunks = Integer.parseInt(args[2]);
    } catch (Exception e) {
      System.out.println("default to chunk=1");
    }
    
    
    Directory smallIdx = buildSmallIndex(f);
    buildLargeIndex(smallIdx, numChunks, outDir);
  }

}
