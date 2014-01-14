package abacus.search.queries;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.IntArrayDocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;
import org.junit.Test;

public class QueriesTest {

  private static Query buildQuery(int[] docs) {
    final IntArrayDocIdSetIterator iter = new IntArrayDocIdSetIterator(docs);
    Filter f = new Filter() {

      @Override
      public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs)
          throws IOException {
        return new DocIdSet() {

          @Override
          public DocIdSetIterator iterator() throws IOException {
            return iter;
          }
          
        };
      }      
    };
    
    return new ConstantScoreQuery(f);
  }
  
  private static IndexReader createFakeReader(int numDocs) throws Exception {
    Directory dir = new RAMDirectory();
    
    IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(Version.LUCENE_46, null));
    int count = 0;
    while (count++ < numDocs) {
      Document doc = new Document();
      writer.addDocument(doc);
    }
    writer.commit();
    writer.close();
    
    return DirectoryReader.open(dir);
  }
  
	@Test
	public void testEarlyTerminateScoreQuery() throws Exception {
	  int[] docs = new int[]{0,1,2,3,4,5,6,7,8,9};
	  Query rawQuery = buildQuery(docs);
	  
	  EarlyTerminateScoreQuery q = new EarlyTerminateScoreQuery(6, 3, 2.0f, rawQuery);
	  
	  IndexReader fakeReader = createFakeReader(docs.length);
	  
	  IndexSearcher searcher = new IndexSearcher(fakeReader);
	  
	  TopDocs td = searcher.search(q, 10);
	  
	  
	  assertEquals(6, td.totalHits);
	  assertEquals(6,td.scoreDocs.length);
	  int numScored = 0;
	  int numNotScored = 0;
	  for (ScoreDoc sd : td.scoreDocs) {
	    if (sd.score == 1.0f) {
	      numScored ++;
	    } else if (sd.score == 2.0f) {
	      numNotScored++;
	    }
	  }
	  
	  assertEquals(3, numScored);
	  assertEquals(3, numNotScored);
	  
	  fakeReader.close();	  
	}
	
	
	
	@Test
	public void testOnlyScoreFirstBatchCollector() throws Exception {
	  int[] docs = new int[]{1,2,3,4,5,6,7,8,9};
    Query rawQuery = buildQuery(docs);
    
    IndexReader fakeReader = createFakeReader(docs.length);
    
    CountOnlyCollector scoredCollector = new CountOnlyCollector();
    CountOnlyCollector otherCollector = new CountOnlyCollector();
    
    OnlyScoreFirstBatchCollector collector = new OnlyScoreFirstBatchCollector(scoredCollector, otherCollector, 7);
    
    IndexSearcher searcher = new IndexSearcher(fakeReader);
    
    searcher.search(rawQuery, collector);
    
    assertEquals(7, scoredCollector.numCollected());
    assertEquals(docs.length, otherCollector.numCollected());
    
    fakeReader.close();
	}

}
