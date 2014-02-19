package abacus.search.facets;

import it.unimi.dsi.fastutil.ints.IntIterator;

public class SingleValueIntIterator implements IntIterator {

  private final int val;
  private boolean endReached;
  public SingleValueIntIterator(int val) {
    this.val = val;
    endReached = false; 
  }

  @Override
  public boolean hasNext() {
    return !endReached;
  }

  @Override
  public Integer next() {
    return nextInt();
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int nextInt() {
    endReached = false;
    return val;
  }

  @Override
  public int skip(int n) {
    throw new UnsupportedOperationException();
  }

}
