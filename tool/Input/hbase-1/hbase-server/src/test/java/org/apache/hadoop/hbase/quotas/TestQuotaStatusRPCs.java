/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.quotas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.Waiter;
import org.apache.hadoop.hbase.Waiter.Predicate;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.master.HMaster;
import org.apache.hadoop.hbase.quotas.SpaceQuotaSnapshot.SpaceQuotaStatus;
import org.apache.hadoop.hbase.quotas.policies.MissingSnapshotViolationPolicyEnforcement;
import org.apache.hadoop.hbase.regionserver.HRegionServer;
import org.apache.hadoop.hbase.testclassification.MediumTests;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

/**
 * Test class for the quota status RPCs in the master and regionserver.
 */
@Category({MediumTests.class})
public class TestQuotaStatusRPCs {
  private static final Log LOG = LogFactory.getLog(TestQuotaStatusRPCs.class);
  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static final AtomicLong COUNTER = new AtomicLong(0);

  @Rule
  public TestName testName = new TestName();
  private SpaceQuotaHelperForTests helper;

  @BeforeClass
  public static void setUp() throws Exception {
    Configuration conf = TEST_UTIL.getConfiguration();
    // Increase the frequency of some of the chores for responsiveness of the test
    SpaceQuotaHelperForTests.updateConfigForQuotas(conf);
    TEST_UTIL.startMiniCluster(1);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    TEST_UTIL.shutdownMiniCluster();
  }

  @Before
  public void setupForTest() throws Exception {
    helper = new SpaceQuotaHelperForTests(TEST_UTIL, testName, COUNTER);
  }

  @Test
  public void testRegionSizesFromMaster() throws Exception {
    final long tableSize = 1024L * 10L; // 10KB
    final int numRegions = 10;
    final TableName tn = helper.createTableWithRegions(numRegions);
    // Will write at least `tableSize` data
    helper.writeData(tn, tableSize);

    final HMaster master = TEST_UTIL.getMiniHBaseCluster().getMaster();
    final MasterQuotaManager quotaManager = master.getMasterQuotaManager();
    // Make sure the master has all of the reports
    Waiter.waitFor(TEST_UTIL.getConfiguration(), 30 * 1000, new Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        Map<HRegionInfo,Long> regionSizes = quotaManager.snapshotRegionSizes();
        LOG.trace("Region sizes=" + regionSizes);
        return numRegions == countRegionsForTable(tn, regionSizes) &&
            tableSize <= getTableSize(tn, regionSizes);
      }
    });

    Map<TableName,Long> sizes = QuotaTableUtil.getMasterReportedTableSizes(TEST_UTIL.getConnection());
    Long size = sizes.get(tn);
    assertNotNull("No reported size for " + tn, size);
    assertTrue("Reported table size was " + size, size.longValue() >= tableSize);
  }

  @Test
  public void testQuotaSnapshotsFromRS() throws Exception {
    final long sizeLimit = 1024L * 1024L; // 1MB
    final long tableSize = 1024L * 10L; // 10KB
    final int numRegions = 10;
    final TableName tn = helper.createTableWithRegions(numRegions);

    // Define the quota
    QuotaSettings settings = QuotaSettingsFactory.limitTableSpace(
        tn, sizeLimit, SpaceViolationPolicy.NO_INSERTS);
    TEST_UTIL.getAdmin().setQuota(settings);

    // Write at least `tableSize` data
    helper.writeData(tn, tableSize);

    final HRegionServer rs = TEST_UTIL.getMiniHBaseCluster().getRegionServer(0);
    final RegionServerSpaceQuotaManager manager = rs.getRegionServerSpaceQuotaManager();
    Waiter.waitFor(TEST_UTIL.getConfiguration(), 30 * 1000, new Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        SpaceQuotaSnapshot snapshot = manager.copyQuotaSnapshots().get(tn);
        if (snapshot == null) {
          return false;
        }
        return snapshot.getUsage() >= tableSize;
      }
    });

    Map<TableName, SpaceQuotaSnapshot> snapshots = QuotaTableUtil.getRegionServerQuotaSnapshots(
        TEST_UTIL.getConnection(), rs.getServerName());
    SpaceQuotaSnapshot snapshot = snapshots.get(tn);
    assertNotNull("Did not find snapshot for " + tn, snapshot);
    assertTrue(
        "Observed table usage was " + snapshot.getUsage(),
        snapshot.getUsage() >= tableSize);
    assertEquals(snapshot.getLimit(), sizeLimit);
    SpaceQuotaStatus pbStatus = snapshot.getQuotaStatus();
    assertFalse(pbStatus.isInViolation());
  }

  @Test
  public void testQuotaEnforcementsFromRS() throws Exception {
    final long sizeLimit = 1024L * 8L; // 8KB
    final long tableSize = 1024L * 10L; // 10KB
    final int numRegions = 10;
    final TableName tn = helper.createTableWithRegions(numRegions);

    // Define the quota
    QuotaSettings settings = QuotaSettingsFactory.limitTableSpace(
        tn, sizeLimit, SpaceViolationPolicy.NO_INSERTS);
    TEST_UTIL.getAdmin().setQuota(settings);

    // Write at least `tableSize` data
    try {
      helper.writeData(tn, tableSize);
    } catch (RetriesExhaustedWithDetailsException | SpaceLimitingException e) {
      // Pass
    }

    final HRegionServer rs = TEST_UTIL.getMiniHBaseCluster().getRegionServer(0);
    final RegionServerSpaceQuotaManager manager = rs.getRegionServerSpaceQuotaManager();
    Waiter.waitFor(TEST_UTIL.getConfiguration(), 30 * 1000, new Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        ActivePolicyEnforcement enforcements = manager.getActiveEnforcements();
        SpaceViolationPolicyEnforcement enforcement = enforcements.getPolicyEnforcement(tn);
        // Signifies that we're waiting on the quota snapshot to be fetched
        if (enforcement instanceof MissingSnapshotViolationPolicyEnforcement) {
          return false;
        }
        return enforcement.getQuotaSnapshot().getQuotaStatus().isInViolation();
      }
    });

    // We obtain the violations for a RegionServer by observing the snapshots
    Map<TableName,SpaceQuotaSnapshot> snapshots =
        QuotaTableUtil.getRegionServerQuotaSnapshots(TEST_UTIL.getConnection(), rs.getServerName());
    SpaceQuotaSnapshot snapshot = snapshots.get(tn);
    assertNotNull("Did not find snapshot for " + tn, snapshot);
    assertTrue(snapshot.getQuotaStatus().isInViolation());
    assertEquals(SpaceViolationPolicy.NO_INSERTS, snapshot.getQuotaStatus().getPolicy());
  }

  @Test
  public void testQuotaStatusFromMaster() throws Exception {
    final long sizeLimit = 1024L * 10L; // 10KB
    final long tableSize = 1024L * 5; // 5KB
    final long nsLimit = Long.MAX_VALUE;
    final int numRegions = 10;
    final TableName tn = helper.createTableWithRegions(numRegions);

    // Define the quota
    QuotaSettings settings = QuotaSettingsFactory.limitTableSpace(
        tn, sizeLimit, SpaceViolationPolicy.NO_INSERTS);
    TEST_UTIL.getAdmin().setQuota(settings);
    QuotaSettings nsSettings = QuotaSettingsFactory.limitNamespaceSpace(
        tn.getNamespaceAsString(), nsLimit, SpaceViolationPolicy.NO_INSERTS);
    TEST_UTIL.getAdmin().setQuota(nsSettings);

    // Write at least `tableSize` data
    helper.writeData(tn, tableSize);

    final Connection conn = TEST_UTIL.getConnection();
    // Make sure the master has a snapshot for our table
    Waiter.waitFor(TEST_UTIL.getConfiguration(), 30 * 1000, new Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        SpaceQuotaSnapshot snapshot = QuotaTableUtil.getCurrentSnapshot(conn, tn);
        LOG.info("Table snapshot after initial ingest: " + snapshot);
        if (snapshot == null) {
          return false;
        }
        return snapshot.getLimit() == sizeLimit && snapshot.getUsage() > 0L;
      }
    });
    final AtomicReference<Long> nsUsage = new AtomicReference<>();
    // If we saw the table snapshot, we should also see the namespace snapshot
    Waiter.waitFor(TEST_UTIL.getConfiguration(), 30 * 1000 * 1000, new Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        SpaceQuotaSnapshot snapshot = QuotaTableUtil.getCurrentSnapshot(
            conn, tn.getNamespaceAsString());
        LOG.debug("Namespace snapshot after initial ingest: " + snapshot);
        if (snapshot == null) {
          return false;
        }
        nsUsage.set(snapshot.getUsage());
        return snapshot.getLimit() == nsLimit && snapshot.getUsage() > 0;
      }
    });

    try {
      helper.writeData(tn, tableSize * 2L);
    } catch (RetriesExhaustedWithDetailsException | SpaceLimitingException e) {
      // Pass
    }

    // Wait for the status to move to violation
    Waiter.waitFor(TEST_UTIL.getConfiguration(), 30 * 1000, new Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        SpaceQuotaSnapshot snapshot = QuotaTableUtil.getCurrentSnapshot(conn, tn);
        LOG.info("Table snapshot after second ingest: " + snapshot);
        if (snapshot == null) {
          return false;
        }
        return snapshot.getQuotaStatus().isInViolation();
      }
    });
    // The namespace should still not be in violation, but have a larger usage than previously
    Waiter.waitFor(TEST_UTIL.getConfiguration(), 30 * 1000, new Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        SpaceQuotaSnapshot snapshot = QuotaTableUtil.getCurrentSnapshot(
            conn, tn.getNamespaceAsString());
        LOG.debug("Namespace snapshot after second ingest: " + snapshot);
        if (snapshot == null) {
          return false;
        }
        return snapshot.getUsage() > nsUsage.get() && !snapshot.getQuotaStatus().isInViolation();
      }
    });
  }

  private int countRegionsForTable(TableName tn, Map<HRegionInfo,Long> regionSizes) {
    int size = 0;
    for (HRegionInfo regionInfo : regionSizes.keySet()) {
      if (tn.equals(regionInfo.getTable())) {
        size++;
      }
    }
    return size;
  }

  private int getTableSize(TableName tn, Map<HRegionInfo,Long> regionSizes) {
    int tableSize = 0;
    for (Entry<HRegionInfo,Long> entry : regionSizes.entrySet()) {
      HRegionInfo regionInfo = entry.getKey();
      long regionSize = entry.getValue();
      if (tn.equals(regionInfo.getTable())) {
        tableSize += regionSize;
      }
    }
    return tableSize;
  }
}
