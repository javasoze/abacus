package abacus.search.facets;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import abacus.indexing.AbacusIndexer;
import abacus.service.AbacusQueryParser;
import abacus.service.AbacusQueryParser.DefaultQueryParser;

public class FacetTestUtil {
  //test data set
  private static final List<Document> DOC_LIST = new ArrayList<Document>();
  static final Directory IDX_DIR = new RAMDirectory();

  static final AbacusQueryParser QUERY_PARSER = new DefaultQueryParser("contents",
      new StandardAnalyzer());

  static {
    Document doc = new Document();
    doc.add(new NumericDocValuesField("id", 1));
    AbacusIndexer.addNumericField(doc, "size", 4);
    AbacusIndexer.addFacetTermField(doc, "color", "red", false);
    AbacusIndexer.addFacetTermField(doc, "tag", "rabbit", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "pet", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "animal", true);
    DOC_LIST.add(doc);

    doc = new Document();
    doc.add(new NumericDocValuesField("id", 2));
    AbacusIndexer.addNumericField(doc, "size", 2);
    AbacusIndexer.addFacetTermField(doc, "color", "red", false);
    AbacusIndexer.addFacetTermField(doc, "tag", "dog", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "pet", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "poodle", true);
    DOC_LIST.add(doc);

    doc = new Document();
    doc.add(new NumericDocValuesField("id", 3));
    AbacusIndexer.addNumericField(doc, "size", 4);
    AbacusIndexer.addFacetTermField(doc, "color", "green", false);
    AbacusIndexer.addFacetTermField(doc, "tag", "rabbit", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "cartoon", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "funny", true);
    DOC_LIST.add(doc);

    doc = new Document();
    doc.add(new NumericDocValuesField("id", 4));
    AbacusIndexer.addNumericField(doc, "size", 1);
    AbacusIndexer.addFacetTermField(doc, "color", "blue", false);
    AbacusIndexer.addFacetTermField(doc, "tag", "store", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "pet", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "animal", true);
    DOC_LIST.add(doc);

    doc = new Document();
    doc.add(new NumericDocValuesField("id", 5));
    AbacusIndexer.addNumericField(doc, "size", 4);
    AbacusIndexer.addFacetTermField(doc, "color", "blue", false);
    AbacusIndexer.addFacetTermField(doc, "tag", "cartoon", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "funny", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "disney", true);
    DOC_LIST.add(doc);

    doc = new Document();
    doc.add(new NumericDocValuesField("id", 6));
    AbacusIndexer.addNumericField(doc, "size", 6);
    AbacusIndexer.addFacetTermField(doc, "color", "green", false);
    AbacusIndexer.addFacetTermField(doc, "tag", "funny", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "humor", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "joke", true);
    DOC_LIST.add(doc);

    doc = new Document();
    doc.add(new NumericDocValuesField("id", 7));
    AbacusIndexer.addNumericField(doc, "size", 2);
    AbacusIndexer.addFacetTermField(doc, "color", "red", false);
    AbacusIndexer.addFacetTermField(doc, "tag", "humane", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "dog", true);
    AbacusIndexer.addFacetTermField(doc, "tag", "rabbit", true);
    DOC_LIST.add(doc);

    IndexWriterConfig conf = new IndexWriterConfig(null);
    try {
      IndexWriter writer = new IndexWriter(IDX_DIR, conf);
      int count = 0;
      for (Document d : DOC_LIST) {
        writer.addDocument(d);
        // make sure we get multiple segments
        if (count % 2 == 1) {
          writer.commit();
        }
        count++;
      }
      writer.commit();

      writer.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static IndexReader getIndexReader(Directory dir, FastDocValuesAtomicReader.MemType memType)
      throws Exception {
    IndexReader reader = DirectoryReader.open(dir);

    List<LeafReaderContext> leaves = reader.leaves();
    LeafReader[] subReaders = new LeafReader[leaves.size()];
    int i = 0;
    for (LeafReaderContext leaf : leaves) {
      LeafReader atomicReader = leaf.reader();
      subReaders[i++] = new FastDocValuesAtomicReader(atomicReader, null, memType);
    }
    reader = new MultiReader(subReaders, true);
    return reader;
  }

  public static void deleteDir(File file) {
    if (file == null || !file.exists()) {
      return;
    }
    for (File f : file.listFiles()) {
      if (f.isDirectory()) {
        deleteDir(f);
      } else {
        f.delete();
      }
    }
    file.delete();
  }

}
