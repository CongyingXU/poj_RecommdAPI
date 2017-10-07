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
package org.apache.hadoop.hbase.snapshot;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.junit.Assert;

public class MobSnapshotTestingUtils {

  /**
   * Create the Mob Table.
   */
  public static void createMobTable(final HBaseTestingUtility util,
      final TableName tableName, int regionReplication,
      final byte[]... families) throws IOException, InterruptedException {
    createMobTable(util, tableName, SnapshotTestingUtils.getSplitKeys(),
      regionReplication, families);
  }

  public static void createPreSplitMobTable(final HBaseTestingUtility util,
      final TableName tableName, int nRegions, final byte[]... families)
      throws IOException, InterruptedException {
    createMobTable(util, tableName, SnapshotTestingUtils.getSplitKeys(nRegions),
      1, families);
  }

  private static void createMobTable(final HBaseTestingUtility util,
      final TableName tableName, final byte[][] splitKeys, int regionReplication,
      final byte[]... families) throws IOException, InterruptedException {
    HTableDescriptor htd = new HTableDescriptor(tableName);
    htd.setRegionReplication(regionReplication);
    for (byte[] family : families) {
      HColumnDescriptor hcd = new HColumnDescriptor(family);
      hcd.setMobEnabled(true);
      hcd.setMobThreshold(0L);
      htd.addFamily(hcd);
    }
    util.getAdmin().createTable(htd, splitKeys);
    SnapshotTestingUtils.waitForTableToBeOnline(util, tableName);
    assertEquals((splitKeys.length + 1) * regionReplication, util
        .getAdmin().getTableRegions(tableName).size());
  }

  /**
   * Create a Mob table.
   *
   * @param util
   * @param tableName
   * @param families
   * @return An HTable instance for the created table.
   * @throws IOException
   */
  public static Table createMobTable(final HBaseTestingUtility util,
      final TableName tableName, final byte[]... families) throws IOException {
    HTableDescriptor htd = new HTableDescriptor(tableName);
    for (byte[] family : families) {
      HColumnDescriptor hcd = new HColumnDescriptor(family);
      // Disable blooms (they are on by default as of 0.95) but we disable them
      // here because
      // tests have hard coded counts of what to expect in block cache, etc.,
      // and blooms being
      // on is interfering.
      hcd.setBloomFilterType(BloomType.NONE);
      hcd.setMobEnabled(true);
      hcd.setMobThreshold(0L);
      htd.addFamily(hcd);
    }
    util.getAdmin().createTable(htd);
    // HBaseAdmin only waits for regions to appear in hbase:meta we should wait
    // until they are assigned
    util.waitUntilAllRegionsAssigned(htd.getTableName());
    return ConnectionFactory.createConnection(util.getConfiguration()).getTable(htd.getTableName());
  }

  /**
   * Return the number of rows in the given table.
   */
  public static int countMobRows(final Table table, final byte[]... families) throws IOException {
    Scan scan = new Scan();
    for (byte[] family : families) {
      scan.addFamily(family);
    }
    try (ResultScanner results = table.getScanner(scan)) {
      int count = 0;
      for (Result res; (res = results.next()) != null;) {
        count++;
        for (Cell cell : res.listCells()) {
          // Verify the value
          Assert.assertTrue(CellUtil.cloneValue(cell).length > 0);
        }
      }
      return count;
    }
  }

  public static void verifyMobRowCount(final HBaseTestingUtility util,
      final TableName tableName, long expectedRows) throws IOException {

    Table table = ConnectionFactory.createConnection(util.getConfiguration()).getTable(tableName);
    try {
      assertEquals(expectedRows, countMobRows(table));
    } finally {
      table.close();
    }
  }

  // ==========================================================================
  // Snapshot Mock
  // ==========================================================================
  public static class SnapshotMock extends SnapshotTestingUtils.SnapshotMock {
    public SnapshotMock(final Configuration conf, final FileSystem fs, final Path rootDir) {
      super(conf, fs, rootDir);
    }

    @Override
    public HTableDescriptor createHtd(final String tableName) {
      HTableDescriptor htd = new HTableDescriptor(TableName.valueOf(tableName));
      HColumnDescriptor hcd = new HColumnDescriptor(TEST_FAMILY);
      hcd.setMobEnabled(true);
      hcd.setMobThreshold(0L);
      htd.addFamily(hcd);
      return htd;
    }
  }
}
