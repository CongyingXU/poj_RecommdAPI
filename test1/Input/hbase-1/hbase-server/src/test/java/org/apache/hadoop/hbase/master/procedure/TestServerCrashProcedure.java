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

package org.apache.hadoop.hbase.master.procedure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.master.HMaster;
import org.apache.hadoop.hbase.master.assignment.AssignmentTestingUtil;
import org.apache.hadoop.hbase.procedure2.ProcedureExecutor;
import org.apache.hadoop.hbase.procedure2.ProcedureMetrics;
import org.apache.hadoop.hbase.procedure2.ProcedureTestingUtility;
import org.apache.hadoop.hbase.testclassification.LargeTests;
import org.apache.hadoop.hbase.testclassification.MasterTests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.experimental.categories.Category;

@Category({MasterTests.class, LargeTests.class})
public class TestServerCrashProcedure {
  private static final Log LOG = LogFactory.getLog(TestServerCrashProcedure.class);

  private HBaseTestingUtility util;

  private ProcedureMetrics serverCrashProcMetrics;
  private long serverCrashSubmittedCount = 0;
  private long serverCrashFailedCount = 0;

  private void setupConf(Configuration conf) {
    conf.setInt(MasterProcedureConstants.MASTER_PROCEDURE_THREADS, 1);
    conf.set("hbase.balancer.tablesOnMaster", "none");
    conf.setInt("hbase.client.retries.number", 3);
  }

  @Before
  public void setup() throws Exception {
    this.util = new HBaseTestingUtility();
    setupConf(this.util.getConfiguration());
    this.util.startMiniCluster(3);
    ProcedureTestingUtility.setKillAndToggleBeforeStoreUpdate(
      this.util.getHBaseCluster().getMaster().getMasterProcedureExecutor(), false);
    serverCrashProcMetrics = this.util.getHBaseCluster().getMaster().getMasterMetrics()
        .getServerCrashProcMetrics();
  }

  @After
  public void tearDown() throws Exception {
    MiniHBaseCluster cluster = this.util.getHBaseCluster();
    HMaster master = cluster == null? null: cluster.getMaster();
    if (master != null && master.getMasterProcedureExecutor() != null) {
      ProcedureTestingUtility.setKillAndToggleBeforeStoreUpdate(
        master.getMasterProcedureExecutor(), false);
    }
    this.util.shutdownMiniCluster();
  }


  @Test(timeout=60000)
  public void testCrashTargetRs() throws Exception {
  }

  @Ignore  // HBASE-18366... To be enabled again.
  @Test(timeout=60000)
  public void testRecoveryAndDoubleExecutionOnRsWithMeta() throws Exception {
    testRecoveryAndDoubleExecution(true);
  }

  @Test(timeout=60000)
  public void testRecoveryAndDoubleExecutionOnRsWithoutMeta() throws Exception {
    testRecoveryAndDoubleExecution(false);
  }

  /**
   * Run server crash procedure steps twice to test idempotency and that we are persisting all
   * needed state.
   * @throws Exception
   */
  private void testRecoveryAndDoubleExecution(final boolean carryingMeta) throws Exception {
    final TableName tableName = TableName.valueOf(
      "testRecoveryAndDoubleExecution-carryingMeta-" + carryingMeta);
    final Table t = this.util.createTable(tableName, HBaseTestingUtility.COLUMNS,
        HBaseTestingUtility.KEYS_FOR_HBA_CREATE_TABLE);
    try {
      // Load the table with a bit of data so some logs to split and some edits in each region.
      this.util.loadTable(t, HBaseTestingUtility.COLUMNS[0]);
      final int count = util.countRows(t);
      assertTrue("expected some rows", count > 0);
      final String checksum = util.checksumRows(t);
      // Run the procedure executor outside the master so we can mess with it. Need to disable
      // Master's running of the server crash processing.
      final HMaster master = this.util.getHBaseCluster().getMaster();
      final ProcedureExecutor<MasterProcedureEnv> procExec = master.getMasterProcedureExecutor();
      master.setServerCrashProcessingEnabled(false);
      // find the first server that match the request and executes the test
      ServerName rsToKill = null;
      for (HRegionInfo hri: util.getHBaseAdmin().getTableRegions(tableName)) {
        final ServerName serverName = AssignmentTestingUtil.getServerHoldingRegion(util, hri);
        if (AssignmentTestingUtil.isServerHoldingMeta(util, serverName) == carryingMeta) {
          rsToKill = serverName;
          break;
        }
      }
      // kill the RS
      AssignmentTestingUtil.killRs(util, rsToKill);
      // Now, reenable processing else we can't get a lock on the ServerCrashProcedure.
      master.setServerCrashProcessingEnabled(true);
      // Do some of the master processing of dead servers so when SCP runs, it has expected 'state'.
      master.getServerManager().moveFromOnlineToDeadServers(rsToKill);
      // Enable test flags and then queue the crash procedure.
      ProcedureTestingUtility.waitNoProcedureRunning(procExec);
      ProcedureTestingUtility.setKillAndToggleBeforeStoreUpdate(procExec, true);
      long procId = procExec.submitProcedure(new ServerCrashProcedure(
          procExec.getEnvironment(), rsToKill, true, carryingMeta));
      // Now run through the procedure twice crashing the executor on each step...
      MasterProcedureTestingUtility.testRecoveryAndDoubleExecution(procExec, procId);
      // Assert all data came back.
      assertEquals(count, util.countRows(t));
      assertEquals(checksum, util.checksumRows(t));
    } finally {
      t.close();
    }
  }

  private void collectMasterMetrics() {
    serverCrashSubmittedCount = serverCrashProcMetrics.getSubmittedCounter().getCount();
    serverCrashFailedCount = serverCrashProcMetrics.getFailedCounter().getCount();
  }
}
