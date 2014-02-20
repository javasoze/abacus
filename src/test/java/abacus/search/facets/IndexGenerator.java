package abacus.search.facets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.json.JSONObject;

public class IndexGenerator {

  static void addMetaString(Document doc, String field, String value) {
    if (value != null) {
      doc.add(new SortedDocValuesField(field, new BytesRef(value)));
      doc.add(new StringField(field+"_indexed", value, Store.YES));
    }
  }
  
  static final String CONTENTS_FIELD = "contents";
  
  static Document buildDoc(JSONObject json) throws Exception{
    Document doc = new Document();
    String[] catchAll = new String[5];
    
    doc.add(new NumericDocValuesField("id", json.getLong("id")));
    doc.add(new DoubleDocValuesField("price", json.optDouble("price")));
    catchAll[0]=String.valueOf(json.optDouble("price"));
    doc.add(new TextField("contents", json.optString("contents"), Store.NO));
    doc.add(new NumericDocValuesField("year", json.optInt("year")));
    catchAll[1]=String.valueOf(json.optInt("year"));
    doc.add(new NumericDocValuesField("mileage", json.optInt("mileage")));
    catchAll[2]=String.valueOf(json.optInt("mileage"));
    addMetaString(doc,"color", json.optString("color"));
    catchAll[3]=String.valueOf(json.optString("color"));
    addMetaString(doc,"category", json.optString("category"));
    catchAll[4]=String.valueOf(json.optString("category"));
    
    for (String s : catchAll) {
      doc.add(new SortedSetDocValuesField("catchall", new BytesRef(s)));
    }
    
    String tagsString = json.optString("tags");
    if (tagsString != null) {
      String[] parts = tagsString.split(",");
      if (parts != null && parts.length > 0) {
        for (String part : parts) {
          doc.add(new SortedSetDocValuesField("tags", new BytesRef(part)));
          doc.add(new StringField("tags_indexed", part, Store.NO));
        }
      }      
    }    
    //doc.add(new BinaryDocValuesField("json", new BytesRef(json.toString())));    
    return doc;
  }
  
  static Directory buildSmallIndex(File dataFile, File idxDir) throws Exception {
    BufferedReader reader = new BufferedReader(new FileReader(dataFile));
    
    
    IndexWriterConfig idxWriterConfig = new IndexWriterConfig(Version.LUCENE_47, new StandardAnalyzer(Version.LUCENE_47));
    Directory dir = FSDirectory.open(idxDir);
    IndexWriter writer = new IndexWriter(dir, idxWriterConfig);
    int count = 0;
    while (true) {
      String line = reader.readLine();
      if (line == null) break;
      
      JSONObject json = new JSONObject(line);
      Document doc = buildDoc(json);
      writer.addDocument(doc);
      count++;
      if (count % 100 == 0) {
        System.out.print(".");
      }
    }
    
    System.out.println(count+" docs indexed");
    
    reader.close();
    writer.commit();
    writer.close();
    
    return dir;
  }
  
  static void buildLargeIndex(Directory smallIndex, int numChunks, File outDir) throws Exception{
    IndexWriterConfig idxWriterConfig = new IndexWriterConfig(Version.LUCENE_47, null);
    Directory tempDir = FSDirectory.open(outDir);
    IndexWriter writer = new IndexWriter(tempDir, idxWriterConfig);
    // build first 1.5M chunk
    for (int i=0; i<100; ++i) {
      writer.addIndexes(smallIndex);
    }
    writer.forceMerge(1);
    writer.commit();
    writer.close();    
  }
  
  
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("usage: source_file index_dir");
    }
    
    File f = new File(args[0]);
    File outDir = new File(args[1]);
    
    
    Directory smallIdx = buildSmallIndex(f, new File(outDir,"small-idx"));
    buildLargeIndex(smallIdx, 10, new File(outDir,"large-idx"));
  }

}
