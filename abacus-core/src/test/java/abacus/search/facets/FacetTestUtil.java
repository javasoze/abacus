package abacus.search.facets;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import abacus.indexing.AbacusIndexer;

public class FacetTestUtil {
   //test dataset
   private static final List<Document> DOC_LIST = new ArrayList<Document>();
   static final Directory IDX_DIR = new RAMDirectory();
   
   static final QueryParser QUERY_PARSER = new QueryParser(Version.LUCENE_48, "contents", 
       new StandardAnalyzer(Version.LUCENE_48)); 
   
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
       
       IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_48, null);
       try {
         IndexWriter writer = new IndexWriter(IDX_DIR, conf);
         int count = 0;
         for (Document d : DOC_LIST) {
           writer.addDocument(d);          
           // make sure we get multiple segments
           if (count %2 == 1) {
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
}
