package org.sonatype.nexus.blobstore.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @since 3.0
 */
public class SimpleLockFactoryTest
    extends TestSupport
{

  public static final BlobId BLOB_ID_A = new BlobId("A");

  public static final BlobId BLOB_ID_B = new BlobId("B");

  private SimpleLockFactory factory;

  private List<LockEvent> events;

  @Before
  public void setUp() {
    factory = new SimpleLockFactory();
    events = Collections.synchronizedList(new ArrayList<LockEvent>());
  }

  @Test
  public void exclusiveLocksAreExclusive() throws Exception {

    final int numberOfThreads = 10;
    final ExecutorService ex = Executors.newFixedThreadPool(numberOfThreads);

    final CyclicBarrier startingGun = new CyclicBarrier(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      final int threadNumber = i;
      ex.submit(new Runnable()
      {
        @Override
        public void run() {
          try {
            log("Runnable {} awaiting starting gun.", threadNumber);
            startingGun.await();
          }
          catch (Exception e) {
            log(e);
            throw new RuntimeException(e);
          }

          try (final BlobLock lock = factory.exclusiveLock(BLOB_ID_A)) {
            logEvent(LockEvent.obtainExclusive(threadNumber));

            Thread.sleep(30);

            // It's important to document the release of the lock just before it actually happens, otherwise log
            // events happen out of order.  TODO: Consider a better pattern for this.
            logEvent(LockEvent.releaseExclusive(threadNumber));
          }
          catch (InterruptedException e) {
            throw new RuntimeException(e);
          }

        }
      });
    }

    log("Main test thread about to await termination.");
    ex.shutdown();
    ex.awaitTermination(60, TimeUnit.SECONDS);

    validateEventLog(events, numberOfThreads);
  }

  @Test
  public void allThreadsCanHoldReadLockSimultaneously() throws Exception {

    final int numberOfThreads = 10;
    final ExecutorService ex = Executors.newFixedThreadPool(numberOfThreads);

    final CyclicBarrier startingGun = new CyclicBarrier(numberOfThreads);

    final CyclicBarrier allThreadsHaveReadLock = new CyclicBarrier(numberOfThreads);

    for (int i = 0; i < numberOfThreads; i++) {
      final int threadNumber = i;
      ex.submit(new Runnable()
      {
        @Override
        public void run() {
          try {
            startingGun.await();

            final BlobLock lock = factory.readLock(BLOB_ID_A);
            logEvent(LockEvent.obtainRead(threadNumber));

            // All threads wait until each has obtained a read lock, proving that read locks can be held simultaneously
            allThreadsHaveReadLock.await();

            logEvent(LockEvent.releaseRead(threadNumber));
            lock.close();
          }
          catch (Exception e) {
            log(e);
            throw new RuntimeException(e);
          }
        }
      });
    }
    ex.shutdown();
    ex.awaitTermination(60, TimeUnit.SECONDS);
  }

  private void validateEventLog(final List<LockEvent> events, final int numberOfLockAcquisitions) {
    assertThat("two events per lock acquisition", events.size(), equalTo(numberOfLockAcquisitions * 2));

    Iterator<LockEvent> eventIterator = events.iterator();

    System.err.println("VALIDATING EVENT LOG");


    while (eventIterator.hasNext()) {
      final LockEvent event = eventIterator.next();

      // If we obtain an exclusive lock, then the very next event must be the same thread releasing it.

      if (event.isObtainingExclusive()) {
        final LockEvent nextEvent = eventIterator.next();
        assertThat(nextEvent.isReleasingExclusive(), equalTo(true));
        assertThat(event.getThreadNum(), equalTo(nextEvent.getThreadNum()));
      }
    }
  }

  private synchronized void logEvent(LockEvent lockEvent) {
    log(lockEvent);
    this.events.add(lockEvent);
    System.err.println(lockEvent);
    System.err.flush();
  }

  private static class LockEvent
  {
    private int threadNum;

    private boolean exclusive;

    private boolean obtaining;

    private static LockEvent obtainExclusive(int threadNumber) {
      return new LockEvent(threadNumber, true, true);
    }

    private static LockEvent obtainRead(int threadNumber) {
      return new LockEvent(threadNumber, false, true);
    }

    private static LockEvent releaseExclusive(int threadNumber) {
      return new LockEvent(threadNumber, true, false);
    }

    private static LockEvent releaseRead(int threadNumber) {
      return new LockEvent(threadNumber, false, false);
    }

    private LockEvent(final int threadNum, final boolean exclusive, final boolean obtaining) {
      this.threadNum = threadNum;
      this.exclusive = exclusive;
      this.obtaining = obtaining;
    }

    public int getThreadNum() {
      return threadNum;
    }

    private boolean isObtainingExclusive() {
      return exclusive && obtaining;
    }

    private boolean isReleasingExclusive() {
      return exclusive && !obtaining;
    }

    @Override
    public String toString() {
      return "LockEvent{thread=" + threadNum + " " + (obtaining ? "obtains" : "releases") + " " +
          (exclusive ? "exclusive" : "read") + " lock}";
    }
  }

}
