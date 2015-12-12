package abacus.search.facets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.json.JSONObject;

import abacus.api.AbacusFieldType;
import abacus.config.FacetIndexedType;
import abacus.config.FieldConfig;
import abacus.config.FieldConfigBuilder;
import abacus.config.IndexDirectoryFacetsConfigReader;
import abacus.indexing.AbacusIndexer;

public class IndexGenerator {

  static void addMetaString(Document doc, String field, String value) {
    if (value != null) {
      AbacusIndexer.addFacetTermField(doc, field, value, false);
      AbacusIndexer.addAttributeField(doc, "attribute", field, value);
    }
  }

  static Map<String, FieldConfig> buildConfig() {
    Map<String, FieldConfig> configMap = new HashMap<>();
    {
      FieldConfigBuilder builder = new FieldConfigBuilder();
      builder.withFacetIndexedType(FacetIndexedType.NUMERIC);
      builder.withFiledType(AbacusFieldType.DOUBLE);
      builder.withFacetIndexedRangeStrings(
          "(* TO 6700]", "[6800 TO 9900]", "[10000 TO 13100]",
          "[13200 TO 17300]", "[17400 TO *)");
      configMap.put("price", builder.build());
    }
    {
      FieldConfigBuilder builder = new FieldConfigBuilder();
      builder.withFacetIndexedType(FacetIndexedType.NUMERIC);
      builder.withFiledType(AbacusFieldType.INT);
      builder.withFacetIndexedRangeStrings(
          "(* TO 1994]", "[1995 TO 1996]",
          "[1997 TO 1998]", "[1999 TO 2000]", "[2001 TO 2002]");
      configMap.put("year", builder.build());
    }
    {
      FieldConfigBuilder builder = new FieldConfigBuilder();
      builder.withFacetIndexedType(FacetIndexedType.NUMERIC);
      builder.withFiledType(AbacusFieldType.FLOAT);
      builder.withFacetIndexedRangeStrings(
          "(* TO 12500]", "(12500 TO 15000]",
          "(15000 TO 17500]", "(17500 TO *)"
      );
      configMap.put("mileage", builder.build());
    }
    {
      FieldConfigBuilder builder = new FieldConfigBuilder();
      builder.withFacetIndexedType(FacetIndexedType.SINGLE);
      builder.withFiledType(AbacusFieldType.STRING);
      configMap.put("color", builder.build());
    }
    {
      FieldConfigBuilder builder = new FieldConfigBuilder();
      builder.withFacetIndexedType(FacetIndexedType.SINGLE);
      builder.withFiledType(AbacusFieldType.STRING);
      configMap.put("category", builder.build());
    }
    {
      FieldConfigBuilder builder = new FieldConfigBuilder();
      builder.withFacetIndexedType(FacetIndexedType.MULTI);
      builder.withFiledType(AbacusFieldType.STRING);
      configMap.put("tags", builder.build());
    }
    {
      FieldConfigBuilder builder = new FieldConfigBuilder();
      builder.withFacetIndexedType(FacetIndexedType.ATTRIBUTE);
      builder.withFiledType(AbacusFieldType.STRING);
      configMap.put("attribute", builder.build());
    }
    return configMap;
  }

  static Document buildDoc(JSONObject json) throws Exception {
    Document doc = new Document();

    // uid
    doc.add(new NumericDocValuesField("id", json.getLong("id")));

    // contents text field
    doc.add(new TextField("contents", json.optString("contents"), Store.NO));

    // range fields
    double price = json.optDouble("price");
    AbacusIndexer.addNumericField(doc, "price", price);
    AbacusIndexer.addAttributeField(doc, "attribute", "price",
        String.valueOf(price));

    int year = json.optInt("year");
    AbacusIndexer.addNumericField(doc, "year", year);
    AbacusIndexer.addAttributeField(doc, "attribute", "year",
        String.valueOf(year));

    float miles = (float) json.optInt("mileage");
    AbacusIndexer.addNumericField(doc, "mileage", miles);
    AbacusIndexer.addAttributeField(doc, "attribute", "mileage",
        String.valueOf(miles));

    addMetaString(doc, "color", json.optString("color"));

    addMetaString(doc, "category", json.optString("category"));

    String tagsString = json.optString("tags");
    if (tagsString != null) {
      String[] parts = tagsString.split(",");
      if (parts != null && parts.length > 0) {
        for (String part : parts) {
          AbacusIndexer.addFacetTermField(doc, "tags", part, true);
          AbacusIndexer.addAttributeField(doc, "attribute", "tags", part);
        }
      }
    }

    AbacusIndexer.addSrcDataField(doc, json.toString());
    return doc;
  }

  static Directory buildSmallIndex(File dataFile) throws Exception {
    BufferedReader reader = new BufferedReader(new FileReader(dataFile));

    IndexWriterConfig idxWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
    Directory dir = new RAMDirectory();
    IndexWriter writer = new IndexWriter(dir, idxWriterConfig);
    int count = 0;
    while (true) {
      String line = reader.readLine();
      if (line == null)
        break;

      JSONObject json = new JSONObject(line);
      Document doc = buildDoc(json);
      writer.addDocument(doc);
      count++;
    }

    System.out.println(count + " seed docs indexed");

    reader.close();

    Map<String, FieldConfig> configMap = buildConfig();
    IndexDirectoryFacetsConfigReader.putFacetsConfig(writer, configMap);
    writer.commit();
    writer.close();

    return dir;
  }

  static void buildLargeIndex(Directory smallIndex, int numChunks, File outDir)
      throws Exception {
    IndexWriterConfig idxWriterConfig = new IndexWriterConfig(null);
    Path idxPath = FileSystems.getDefault().getPath(outDir.getAbsolutePath());
    Directory tempDir = FSDirectory.open(idxPath);
    IndexWriter writer = new IndexWriter(tempDir, idxWriterConfig);
    // index size = 15000 * numChunks
    DirectoryReader reader = DirectoryReader.open(smallIndex);
    Map<String, String> commitData = reader.getIndexCommit().getUserData();
    for (int i = 0; i < numChunks; ++i) {      
      writer.addIndexes(smallIndex);
      System.out.println((i + 1) * reader.maxDoc() + " docs indexed.");
    }

    System.out.println("merging all segments");
    writer.forceMerge(1);
    System.out.println("saving commit data: " + commitData);
    writer.setCommitData(commitData);
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
