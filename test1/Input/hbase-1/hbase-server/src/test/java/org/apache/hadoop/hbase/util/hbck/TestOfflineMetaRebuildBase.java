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
package org.apache.hadoop.hbase.util.hbck;

import static org.apache.hadoop.hbase.util.hbck.HbckTestingUtil.assertErrors;
import static org.apache.hadoop.hbase.util.hbck.HbckTestingUtil.doFsck;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.testclassification.MediumTests;
import org.apache.hadoop.hbase.testclassification.MiscTests;
import org.apache.hadoop.hbase.util.FSUtils;
import org.apache.hadoop.hbase.util.HBaseFsck;
import org.apache.hadoop.hbase.util.HBaseFsck.ErrorReporter.ERROR_CODE;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
/**
 * This builds a table, removes info from meta, and then rebuilds meta.
 */
@Category({MiscTests.class, MediumTests.class})
public class TestOfflineMetaRebuildBase extends OfflineMetaRebuildTestCore {
  private static final Log LOG = LogFactory.getLog(TestOfflineMetaRebuildBase.class);

  @SuppressWarnings("deprecation")
  @Ignore @Test(timeout = 120000) // To fix post HBASE-14614
  public void testMetaRebuild() throws Exception {
    wipeOutMeta();

    // is meta really messed up?
    assertEquals(1, scanMeta());
    assertErrors(doFsck(conf, false),
        new ERROR_CODE[] {
            ERROR_CODE.NOT_IN_META_OR_DEPLOYED,
            ERROR_CODE.NOT_IN_META_OR_DEPLOYED,
            ERROR_CODE.NOT_IN_META_OR_DEPLOYED,
            ERROR_CODE.NOT_IN_META_OR_DEPLOYED});
    // Note, would like to check # of tables, but this takes a while to time
    // out.

    // shutdown the minicluster
    TEST_UTIL.shutdownMiniHBaseCluster();
    TEST_UTIL.shutdownMiniZKCluster();

    // rebuild meta table from scratch
    HBaseFsck fsck = new HBaseFsck(conf);
    assertTrue(fsck.rebuildMeta(false));
    assertTrue("HBCK meta recovery WAL directory exist.", validateHBCKMetaRecoveryWALDir());

    // bring up the minicluster
    TEST_UTIL.startMiniZKCluster();
    TEST_UTIL.restartHBaseCluster(3);
    try (Connection connection = ConnectionFactory.createConnection(TEST_UTIL.getConfiguration())) {
      Admin admin = connection.getAdmin();
      if (admin.isTableDisabled(table))
        admin.enableTable(table);
      LOG.info("Waiting for no more RIT");
      TEST_UTIL.waitUntilNoRegionsInTransition(60000);
      LOG.info("No more RIT in ZK, now doing final test verification");

      // everything is good again.
      assertEquals(5, scanMeta()); // including table state rows
      TableName[] tableNames = TEST_UTIL.getAdmin().listTableNames();
      for (TableName tableName : tableNames) {
        HTableDescriptor tableDescriptor = TEST_UTIL.getAdmin().getTableDescriptor(tableName);
        assertNotNull(tableDescriptor);
        assertTrue(TEST_UTIL.getAdmin().isTableEnabled(tableName));
      }
      HTableDescriptor[] htbls = admin.listTables();
      LOG.info("Tables present after restart: " + Arrays.toString(htbls));
      assertEquals(1, htbls.length);
    }

    assertErrors(doFsck(conf, false), new ERROR_CODE[] {});
    LOG.info("Table " + table + " has " + tableRowCount(conf, table) + " entries.");
    assertEquals(16, tableRowCount(conf, table));
  }

  /**
   * Validate whether Meta recovery empty WAL directory is removed.
   * @return True if directory is removed otherwise false.
   */
  private boolean validateHBCKMetaRecoveryWALDir() throws IOException {
    Path rootdir = FSUtils.getRootDir(TEST_UTIL.getConfiguration());
    Path walLogDir = new Path(rootdir, HConstants.HREGION_LOGDIR_NAME);
    FileSystem fs = TEST_UTIL.getTestFileSystem();
    FileStatus[] walFiles = FSUtils.listStatus(fs, walLogDir, null);
    assertNotNull(walFiles);
    for (FileStatus fsStat : walFiles) {
      if (fsStat.isDirectory() && fsStat.getPath().getName().startsWith("hbck-meta-recovery-")) {
        return false;
      }
    }
    return true;
  }
}
