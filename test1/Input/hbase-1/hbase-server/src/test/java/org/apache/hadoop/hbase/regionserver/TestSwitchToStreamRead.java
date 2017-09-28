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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.regionserver.HRegion.RegionScannerImpl;
import org.apache.hadoop.hbase.testclassification.MediumTests;
import org.apache.hadoop.hbase.testclassification.RegionServerTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({ RegionServerTests.class, MediumTests.class })
public class TestSwitchToStreamRead {

  private static final HBaseTestingUtility UTIL = new HBaseTestingUtility();

  private static TableName TABLE_NAME = TableName.valueOf("stream");

  private static byte[] FAMILY = Bytes.toBytes("cf");

  private static byte[] QUAL = Bytes.toBytes("cq");

  private static String VALUE_PREFIX;

  private static HRegion REGION;

  @BeforeClass
  public static void setUp() throws IOException {
    UTIL.getConfiguration().setLong(StoreScanner.STORESCANNER_PREAD_MAX_BYTES, 2048);
    StringBuilder sb = new StringBuilder(256);
    for (int i = 0; i < 255; i++) {
      sb.append((char) ThreadLocalRandom.current().nextInt('A', 'z' + 1));
    }
    VALUE_PREFIX = sb.append("-").toString();
    REGION = UTIL.createLocalHRegion(
      new HTableDescriptor(TABLE_NAME).addFamily(new HColumnDescriptor(FAMILY).setBlocksize(1024)),
      null, null);
    for (int i = 0; i < 900; i++) {
      REGION
          .put(new Put(Bytes.toBytes(i)).addColumn(FAMILY, QUAL, Bytes.toBytes(VALUE_PREFIX + i)));
    }
    REGION.flush(true);
    for (int i = 900; i < 1000; i++) {
      REGION
          .put(new Put(Bytes.toBytes(i)).addColumn(FAMILY, QUAL, Bytes.toBytes(VALUE_PREFIX + i)));
    }
  }

  @AfterClass
  public static void tearDown() throws IOException {
    REGION.close(true);
    UTIL.cleanupTestDir();
  }

  @Test
  public void test() throws IOException {
    try (RegionScanner scanner = REGION.getScanner(new Scan())) {
      StoreScanner storeScanner = (StoreScanner) ((RegionScannerImpl) scanner)
          .getStoreHeapForTesting().getCurrentForTesting();
      for (KeyValueScanner kvs : storeScanner.getAllScannersForTesting()) {
        if (kvs instanceof StoreFileScanner) {
          StoreFileScanner sfScanner = (StoreFileScanner) kvs;
          // starting from pread so we use shared reader here.
          assertTrue(sfScanner.getReader().shared);
        }
      }
      List<Cell> cells = new ArrayList<>();
      for (int i = 0; i < 500; i++) {
        assertTrue(scanner.next(cells));
        Result result = Result.create(cells);
        assertEquals(VALUE_PREFIX + i, Bytes.toString(result.getValue(FAMILY, QUAL)));
        cells.clear();
        scanner.shipped();
      }
      for (KeyValueScanner kvs : storeScanner.getAllScannersForTesting()) {
        if (kvs instanceof StoreFileScanner) {
          StoreFileScanner sfScanner = (StoreFileScanner) kvs;
          // we should have convert to use stream read now.
          assertFalse(sfScanner.getReader().shared);
        }
      }
      for (int i = 500; i < 1000; i++) {
        assertEquals(i != 999, scanner.next(cells));
        Result result = Result.create(cells);
        assertEquals(VALUE_PREFIX + i, Bytes.toString(result.getValue(FAMILY, QUAL)));
        cells.clear();
        scanner.shipped();
      }
    }
    // make sure all scanners are closed.
    for (StoreFile sf : REGION.getStore(FAMILY).getStorefiles()) {
      assertFalse(sf.isReferencedInReads());
    }
  }
}
