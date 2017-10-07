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
package org.apache.hadoop.hbase.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.ClusterStatus;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.RegionLoad;
import org.apache.hadoop.hbase.ServerLoad;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.regionserver.HRegionServer;
import org.apache.hadoop.hbase.regionserver.Region;
import org.apache.hadoop.hbase.testclassification.ClientTests;
import org.apache.hadoop.hbase.testclassification.MediumTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.wal.AbstractFSWALProvider;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@RunWith(Parameterized.class)
@Category({ ClientTests.class, MediumTests.class })
public class TestAsyncClusterAdminApi extends TestAsyncAdminBase {

  private final Path cnfPath = FileSystems.getDefault().getPath("target/test-classes/hbase-site.xml");
  private final Path cnf2Path = FileSystems.getDefault().getPath("target/test-classes/hbase-site2.xml");
  private final Path cnf3Path = FileSystems.getDefault().getPath("target/test-classes/hbase-site3.xml");

  @Test
  public void testRegionServerOnlineConfigChange() throws Exception {
    replaceHBaseSiteXML();
    admin.getRegionServers().get().forEach(server -> admin.updateConfiguration(server).join());

    // Check the configuration of the RegionServers
    TEST_UTIL.getMiniHBaseCluster().getRegionServerThreads().forEach(thread -> {
      Configuration conf = thread.getRegionServer().getConfiguration();
      assertEquals(1000, conf.getInt("hbase.custom.config", 0));
    });

    restoreHBaseSiteXML();
  }

  @Test
  public void testMasterOnlineConfigChange() throws Exception {
    replaceHBaseSiteXML();
    ServerName master = admin.getMaster().get();
    admin.updateConfiguration(master).join();
    admin.getBackupMasters().get()
        .forEach(backupMaster -> admin.updateConfiguration(backupMaster).join());

    // Check the configuration of the Masters
    TEST_UTIL.getMiniHBaseCluster().getMasterThreads().forEach(thread -> {
      Configuration conf = thread.getMaster().getConfiguration();
      assertEquals(1000, conf.getInt("hbase.custom.config", 0));
    });

    restoreHBaseSiteXML();
  }

  @Test
  public void testAllClusterOnlineConfigChange() throws IOException {
    replaceHBaseSiteXML();
    admin.updateConfiguration().join();

    // Check the configuration of the Masters
    TEST_UTIL.getMiniHBaseCluster().getMasterThreads().forEach(thread -> {
      Configuration conf = thread.getMaster().getConfiguration();
      assertEquals(1000, conf.getInt("hbase.custom.config", 0));
    });

    // Check the configuration of the RegionServers
    TEST_UTIL.getMiniHBaseCluster().getRegionServerThreads().forEach(thread -> {
      Configuration conf = thread.getRegionServer().getConfiguration();
      assertEquals(1000, conf.getInt("hbase.custom.config", 0));
    });

    restoreHBaseSiteXML();
  }

  private void replaceHBaseSiteXML() throws IOException {
    // make a backup of hbase-site.xml
    Files.copy(cnfPath, cnf3Path, StandardCopyOption.REPLACE_EXISTING);
    // update hbase-site.xml by overwriting it
    Files.copy(cnf2Path, cnfPath, StandardCopyOption.REPLACE_EXISTING);
  }

  private void restoreHBaseSiteXML() throws IOException {
    // restore hbase-site.xml
    Files.copy(cnf3Path, cnfPath, StandardCopyOption.REPLACE_EXISTING);
  }

  @Test
  public void testRollWALWALWriter() throws Exception {
    setUpforLogRolling();
    String className = this.getClass().getName();
    StringBuilder v = new StringBuilder(className);
    while (v.length() < 1000) {
      v.append(className);
    }
    byte[] value = Bytes.toBytes(v.toString());
    HRegionServer regionServer = startAndWriteData(tableName, value);
    LOG.info("after writing there are "
        + AbstractFSWALProvider.getNumRolledLogFiles(regionServer.getWAL(null)) + " log files");

    // flush all regions
    for (Region r : regionServer.getOnlineRegionsLocalContext()) {
      r.flush(true);
    }
    admin.rollWALWriter(regionServer.getServerName()).join();
    int count = AbstractFSWALProvider.getNumRolledLogFiles(regionServer.getWAL(null));
    LOG.info("after flushing all regions and rolling logs there are " +
        count + " log files");
    assertTrue(("actual count: " + count), count <= 2);
  }

  private void setUpforLogRolling() {
    // Force a region split after every 768KB
    TEST_UTIL.getConfiguration().setLong(HConstants.HREGION_MAX_FILESIZE,
        768L * 1024L);

    // We roll the log after every 32 writes
    TEST_UTIL.getConfiguration().setInt("hbase.regionserver.maxlogentries", 32);

    TEST_UTIL.getConfiguration().setInt(
        "hbase.regionserver.logroll.errors.tolerated", 2);
    TEST_UTIL.getConfiguration().setInt("hbase.rpc.timeout", 10 * 1000);

    // For less frequently updated regions flush after every 2 flushes
    TEST_UTIL.getConfiguration().setInt(
        "hbase.hregion.memstore.optionalflushcount", 2);

    // We flush the cache after every 8192 bytes
    TEST_UTIL.getConfiguration().setInt(HConstants.HREGION_MEMSTORE_FLUSH_SIZE,
        8192);

    // Increase the amount of time between client retries
    TEST_UTIL.getConfiguration().setLong("hbase.client.pause", 10 * 1000);

    // Reduce thread wake frequency so that other threads can get
    // a chance to run.
    TEST_UTIL.getConfiguration().setInt(HConstants.THREAD_WAKE_FREQUENCY,
        2 * 1000);

    /**** configuration for testLogRollOnDatanodeDeath ****/
    // lower the namenode & datanode heartbeat so the namenode
    // quickly detects datanode failures
    TEST_UTIL.getConfiguration().setInt("dfs.namenode.heartbeat.recheck-interval", 5000);
    TEST_UTIL.getConfiguration().setInt("dfs.heartbeat.interval", 1);
    // the namenode might still try to choose the recently-dead datanode
    // for a pipeline, so try to a new pipeline multiple times
    TEST_UTIL.getConfiguration().setInt("dfs.client.block.write.retries", 30);
    TEST_UTIL.getConfiguration().setInt(
        "hbase.regionserver.hlog.tolerable.lowreplication", 2);
    TEST_UTIL.getConfiguration().setInt(
        "hbase.regionserver.hlog.lowreplication.rolllimit", 3);
  }

  private HRegionServer startAndWriteData(TableName tableName, byte[] value) throws Exception {
    createTableWithDefaultConf(tableName);
    RawAsyncTable table = ASYNC_CONN.getRawTable(tableName);
    HRegionServer regionServer = TEST_UTIL.getRSForFirstRegionInTable(tableName);
    for (int i = 1; i <= 256; i++) { // 256 writes should cause 8 log rolls
      Put put = new Put(Bytes.toBytes("row" + String.format("%1$04d", i)));
      put.addColumn(FAMILY, null, value);
      table.put(put).join();
      if (i % 32 == 0) {
        // After every 32 writes sleep to let the log roller run
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e) {
          // continue
        }
      }
    }
    return regionServer;
  }

  @Test
  public void testGetRegionLoads() throws Exception {
    // Turn off the balancer
    admin.setBalancerOn(false).join();
    TableName[] tables =
        new TableName[] { TableName.valueOf(tableName.getNameAsString() + "1"),
            TableName.valueOf(tableName.getNameAsString() + "2"),
            TableName.valueOf(tableName.getNameAsString() + "3") };
    createAndLoadTable(tables);
    // Sleep to wait region server report
    Thread.sleep(TEST_UTIL.getConfiguration().getInt("hbase.regionserver.msginterval", 3 * 1000) * 2);
    // Check if regions match with the regionLoad from the server
    Collection<ServerName> servers = admin.getRegionServers().get();
    for (ServerName serverName : servers) {
      List<HRegionInfo> regions = admin.getOnlineRegions(serverName).get();
      checkRegionsAndRegionLoads(regions, admin.getRegionLoads(serverName).get());
    }

    // Check if regionLoad matches the table's regions and nothing is missed
    for (TableName table : tables) {
      List<HRegionInfo> tableRegions = admin.getTableRegions(table).get();
      List<RegionLoad> regionLoads = Lists.newArrayList();
      for (ServerName serverName : servers) {
        regionLoads.addAll(admin.getRegionLoads(serverName, Optional.of(table)).get());
      }
      checkRegionsAndRegionLoads(tableRegions, regionLoads);
    }

    // Check RegionLoad matches the regionLoad from ClusterStatus
    ClusterStatus clusterStatus = admin.getClusterStatus().get();
    for (ServerName serverName : clusterStatus.getServers()) {
      ServerLoad serverLoad = clusterStatus.getLoad(serverName);
      compareRegionLoads(serverLoad.getRegionsLoad().values(), admin.getRegionLoads(serverName)
          .get());
    }
  }

  private void compareRegionLoads(Collection<RegionLoad> regionLoadCluster,
      Collection<RegionLoad> regionLoads) {

    assertEquals("No of regionLoads from clusterStatus and regionloads from RS doesn't match",
      regionLoadCluster.size(), regionLoads.size());

    for (RegionLoad loadCluster : regionLoadCluster) {
      boolean matched = false;
      for (RegionLoad load : regionLoads) {
        if (Bytes.equals(loadCluster.getName(), load.getName())) {
          matched = true;
          continue;
        }
      }
      assertTrue("The contents of region load from cluster and server should match", matched);
    }
  }

  private void checkRegionsAndRegionLoads(Collection<HRegionInfo> regions,
      Collection<RegionLoad> regionLoads) {

    assertEquals("No of regions and regionloads doesn't match", regions.size(), regionLoads.size());

    Map<byte[], RegionLoad> regionLoadMap = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
    for (RegionLoad regionLoad : regionLoads) {
      regionLoadMap.put(regionLoad.getName(), regionLoad);
    }
    for (HRegionInfo info : regions) {
      assertTrue("Region not in regionLoadMap region:" + info.getRegionNameAsString()
          + " regionMap: " + regionLoadMap, regionLoadMap.containsKey(info.getRegionName()));
    }
  }

  private void createAndLoadTable(TableName[] tables) {
    for (TableName table : tables) {
      TableDescriptorBuilder builder = TableDescriptorBuilder.newBuilder(table);
      builder.addColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(FAMILY).build());
      admin.createTable(builder.build(), Bytes.toBytes("aaaaa"), Bytes.toBytes("zzzzz"), 16).join();
      RawAsyncTable asyncTable = ASYNC_CONN.getRawTable(table);
      List<Put> puts = new ArrayList<>();
      for (byte[] row : HBaseTestingUtility.ROWS) {
        puts.add(new Put(row).addColumn(FAMILY, Bytes.toBytes("q"), Bytes.toBytes("v")));
      }
      asyncTable.putAll(puts).join();
    }
  }
}
