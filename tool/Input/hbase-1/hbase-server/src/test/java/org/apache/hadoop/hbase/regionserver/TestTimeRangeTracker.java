/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.regionserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;

import org.apache.hadoop.hbase.io.TimeRange;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.apache.hadoop.hbase.util.Writables;
import org.apache.hadoop.hbase.testclassification.RegionServerTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.ThreadLocalRandom;

@Category({RegionServerTests.class, SmallTests.class})
public class TestTimeRangeTracker {
  private static final int NUM_KEYS = 10000000;

  @Test
  public void testExtreme() {
    TimeRange tr = new TimeRange();
    assertTrue(tr.includesTimeRange(new TimeRange()));
    TimeRangeTracker trt = new TimeRangeTracker();
    assertFalse(trt.includesTimeRange(new TimeRange()));
    trt.includeTimestamp(1);
    trt.includeTimestamp(10);
    assertTrue(trt.includesTimeRange(new TimeRange()));
  }

  @Test
  public void testTimeRangeInitialized() {
    TimeRangeTracker src = new TimeRangeTracker();
    TimeRange tr = new TimeRange(System.currentTimeMillis());
    assertFalse(src.includesTimeRange(tr));
  }

  @Test
  public void testTimeRangeTrackerNullIsSameAsTimeRangeNull() throws IOException {
    TimeRangeTracker src = new TimeRangeTracker(1, 2);
    byte [] bytes = Writables.getBytes(src);
    TimeRange tgt = TimeRangeTracker.getTimeRange(bytes);
    assertEquals(src.getMin(), tgt.getMin());
    assertEquals(src.getMax(), tgt.getMax());
  }

  @Test
  public void testSerialization() throws IOException {
    TimeRangeTracker src = new TimeRangeTracker(1, 2);
    TimeRangeTracker tgt = new TimeRangeTracker();
    Writables.copyWritable(src, tgt);
    assertEquals(src.getMin(), tgt.getMin());
    assertEquals(src.getMax(), tgt.getMax());
  }

  @Test
  public void testAlwaysDecrementingSetsMaximum() {
    TimeRangeTracker trr = new TimeRangeTracker();
    trr.includeTimestamp(3);
    trr.includeTimestamp(2);
    trr.includeTimestamp(1);
    assertTrue(trr.getMin() != TimeRangeTracker.INITIAL_MIN_TIMESTAMP);
    assertTrue(trr.getMax() != -1 /*The initial max value*/);
  }

  @Test
  public void testSimpleInRange() {
    TimeRangeTracker trr = new TimeRangeTracker();
    trr.includeTimestamp(0);
    trr.includeTimestamp(2);
    assertTrue(trr.includesTimeRange(new TimeRange(1)));
  }

  /**
   * Run a bunch of threads against a single TimeRangeTracker and ensure we arrive
   * at right range.  Here we do ten threads each incrementing over 100k at an offset
   * of the thread index; max is 10 * 10k and min is 0.
   * @throws InterruptedException
   */
  @Test
  public void testArriveAtRightAnswer() throws InterruptedException {
    final TimeRangeTracker trr = new TimeRangeTracker();
    final int threadCount = 10;
    final int calls = 1000 * 1000;
    Thread [] threads = new Thread[threadCount];
    for (int i = 0; i < threads.length; i++) {
      Thread t = new Thread("" + i) {
        @Override
        public void run() {
          int offset = Integer.parseInt(getName());
          boolean even = offset % 2 == 0;
          if (even) {
            for (int i = (offset * calls); i < calls; i++) trr.includeTimestamp(i);
          } else {
            int base = offset * calls;
            for (int i = base + calls; i >= base; i--) trr.includeTimestamp(i);
          }
        }
      };
      t.start();
      threads[i] = t;
    }
    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }

    assertTrue(trr.getMax() == calls * threadCount);
    assertTrue(trr.getMin() == 0);
  }

  @Test
  public void testRangeConstruction() throws IOException {
    TimeRange defaultRange = new TimeRange();
    assertEquals(0L, defaultRange.getMin());
    assertEquals(Long.MAX_VALUE, defaultRange.getMax());
    assertTrue(defaultRange.isAllTime());

    TimeRange oneArgRange = new TimeRange(0L);
    assertEquals(0L, oneArgRange.getMin());
    assertEquals(Long.MAX_VALUE, oneArgRange.getMax());
    assertTrue(oneArgRange.isAllTime());

    TimeRange oneArgRange2 = new TimeRange(1);
    assertEquals(1, oneArgRange2.getMin());
    assertEquals(Long.MAX_VALUE, oneArgRange2.getMax());
    assertFalse(oneArgRange2.isAllTime());

    TimeRange twoArgRange = new TimeRange(0L, Long.MAX_VALUE);
    assertEquals(0L, twoArgRange.getMin());
    assertEquals(Long.MAX_VALUE, twoArgRange.getMax());
    assertTrue(twoArgRange.isAllTime());

    TimeRange twoArgRange2 = new TimeRange(0L, Long.MAX_VALUE - 1);
    assertEquals(0L, twoArgRange2.getMin());
    assertEquals(Long.MAX_VALUE - 1, twoArgRange2.getMax());
    assertFalse(twoArgRange2.isAllTime());

    TimeRange twoArgRange3 = new TimeRange(1, Long.MAX_VALUE);
    assertEquals(1, twoArgRange3.getMin());
    assertEquals(Long.MAX_VALUE, twoArgRange3.getMax());
    assertFalse(twoArgRange3.isAllTime());
  }

  final static int NUM_OF_THREADS = 20;

  class RandomTestData {
    private long[] keys = new long[NUM_KEYS];
    private long min = Long.MAX_VALUE;
    private long max = 0;

    public RandomTestData() {
      if (ThreadLocalRandom.current().nextInt(NUM_OF_THREADS) % 2 == 0) {
        for (int i = 0; i < NUM_KEYS; i++) {
          keys[i] = i + ThreadLocalRandom.current().nextLong(NUM_OF_THREADS);
          if (keys[i] < min) min = keys[i];
          if (keys[i] > max) max = keys[i];
        }
      } else {
        for (int i = NUM_KEYS - 1; i >= 0; i--) {
          keys[i] = i + ThreadLocalRandom.current().nextLong(NUM_OF_THREADS);
          if (keys[i] < min) min = keys[i];
          if (keys[i] > max) max = keys[i];
        }
      }
    }

    public long getMax() {
      return this.max;
    }

    public long getMin() {
      return this.min;
    }
  }

  class TrtUpdateRunnable implements Runnable {

    private TimeRangeTracker trt;
    private RandomTestData data;
    public TrtUpdateRunnable(final TimeRangeTracker trt, final RandomTestData data) {
      this.trt = trt;
      this.data = data;
    }

    public void run() {
      for (long key : data.keys) {
        trt.includeTimestamp(key);
      }
    }
  }

  /**
   * Run a bunch of threads against a single TimeRangeTracker and ensure we arrive
   * at right range.  The data chosen is going to ensure that there are lots collisions, i.e,
   * some other threads may already update the value while one tries to update min/max value.
   */
  @Test
  public void testConcurrentIncludeTimestampCorrectness() {
    RandomTestData[] testData = new RandomTestData[NUM_OF_THREADS];
    long min = Long.MAX_VALUE, max = 0;
    for (int i = 0; i < NUM_OF_THREADS; i ++) {
      testData[i] = new RandomTestData();
      if (testData[i].getMin() < min) {
        min = testData[i].getMin();
      }
      if (testData[i].getMax() > max) {
        max = testData[i].getMax();
      }
    }

    TimeRangeTracker trt = new TimeRangeTracker();

    Thread[] t = new Thread[NUM_OF_THREADS];
    for (int i = 0; i < NUM_OF_THREADS; i++) {
      t[i] = new Thread(new TrtUpdateRunnable(trt, testData[i]));
      t[i].start();
    }

    for (Thread thread : t) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    assertTrue(min == trt.getMin());
    assertTrue(max == trt.getMax());
  }

  /**
   * Bit of code to test concurrent access on this class.
   * @param args
   * @throws InterruptedException
   */
  public static void main(String[] args) throws InterruptedException {
    long start = System.currentTimeMillis();
    final TimeRangeTracker trr = new TimeRangeTracker();
    final int threadCount = 5;
    final int calls = 1024 * 1024 * 128;
    Thread [] threads = new Thread[threadCount];
    for (int i = 0; i < threads.length; i++) {
      Thread t = new Thread("" + i) {
        @Override
        public void run() {
          for (int i = 0; i < calls; i++) trr.includeTimestamp(i);
        }
      };
      t.start();
      threads[i] = t;
    }
    for (int i = 0; i < threads.length; i++) {
      threads[i].join();
    }
    System.out.println(trr.getMin() + " " + trr.getMax() + " " +
      (System.currentTimeMillis() - start));
  }
}
