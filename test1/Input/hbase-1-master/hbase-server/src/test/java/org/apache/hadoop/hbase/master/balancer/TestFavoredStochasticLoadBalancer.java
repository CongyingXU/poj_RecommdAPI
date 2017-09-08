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
package org.apache.hadoop.hbase.master.balancer;

import static org.apache.hadoop.hbase.ServerName.NON_STARTCODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MiniHBaseCluster;
import org.apache.hadoop.hbase.Waiter;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.favored.FavoredNodeAssignmentHelper;
import org.apache.hadoop.hbase.favored.FavoredNodesPlan;
import org.apache.hadoop.hbase.master.HMaster;
import org.apache.hadoop.hbase.master.RegionState;
import org.apache.hadoop.hbase.master.assignment.RegionStates;
import org.apache.hadoop.hbase.master.assignment.RegionStates.RegionStateNode;
import org.apache.hadoop.hbase.master.ServerManager;
import org.apache.hadoop.hbase.regionserver.Region;
import org.apache.hadoop.hbase.testclassification.MediumTests;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.favored.FavoredNodesManager;
import org.apache.hadoop.hbase.master.LoadBalancer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.JVMClusterUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Category(MediumTests.class)
public class TestFavoredStochasticLoadBalancer extends BalancerTestBase {

  private static final Log LOG = LogFactory.getLog(TestFavoredStochasticLoadBalancer.class);

  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static final int SLAVES = 8;
  private static final int REGION_NUM = SLAVES * 3;

  private Admin admin;
  private HMaster master;
  private MiniHBaseCluster cluster;

  @BeforeClass
  public static void setupBeforeClass() throws Exception {
    Configuration conf = TEST_UTIL.getConfiguration();
    // Enable the favored nodes based load balancer
    conf.setClass(HConstants.HBASE_MASTER_LOADBALANCER_CLASS,
        LoadOnlyFavoredStochasticBalancer.class, LoadBalancer.class);
  }

  @Before
  public void startCluster() throws Exception {
    TEST_UTIL.startMiniCluster(SLAVES);
    TEST_UTIL.getDFSCluster().waitClusterUp();
    cluster = TEST_UTIL.getMiniHBaseCluster();
    master = TEST_UTIL.getMiniHBaseCluster().getMaster();
    admin = TEST_UTIL.getAdmin();
    admin.setBalancerRunning(false, true);
  }

  @After
  public void stopCluster() throws Exception {
    TEST_UTIL.cleanupTestDir();
    TEST_UTIL.shutdownMiniCluster();
  }

  @Test
  public void testBasicBalance() throws Exception {

    TableName tableName = TableName.valueOf("testBasicBalance");
    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc, Bytes.toBytes("aaa"), Bytes.toBytes("zzz"), REGION_NUM);
    TEST_UTIL.waitTableAvailable(tableName);
    TEST_UTIL.loadTable(admin.getConnection().getTable(tableName), HConstants.CATALOG_FAMILY);
    admin.flush(tableName);
    compactTable(tableName);

    JVMClusterUtil.RegionServerThread rs1 = cluster.startRegionServerAndWait(10000);
    JVMClusterUtil.RegionServerThread rs2 = cluster.startRegionServerAndWait(10000);

    // Now try to run balance, and verify no regions are moved to the 2 region servers recently
    // started.
    admin.setBalancerRunning(true, true);
    assertTrue("Balancer did not run", admin.balancer());
    TEST_UTIL.waitUntilNoRegionsInTransition(120000);

    List<HRegionInfo> hris = admin.getOnlineRegions(rs1.getRegionServer().getServerName());
    for (HRegionInfo hri : hris) {
      assertFalse("New RS contains regions belonging to table: " + tableName,
        hri.getTable().equals(tableName));
    }
    hris = admin.getOnlineRegions(rs2.getRegionServer().getServerName());
    for (HRegionInfo hri : hris) {
      assertFalse("New RS contains regions belonging to table: " + tableName,
        hri.getTable().equals(tableName));
    }
  }

  @Test
  public void testRoundRobinAssignment() throws Exception {

    TableName tableName = TableName.valueOf("testRoundRobinAssignment");
    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc, Bytes.toBytes("aaa"), Bytes.toBytes("zzz"), REGION_NUM);
    TEST_UTIL.waitTableAvailable(tableName);
    TEST_UTIL.loadTable(admin.getConnection().getTable(tableName), HConstants.CATALOG_FAMILY);
    admin.flush(tableName);

    LoadBalancer balancer = master.getLoadBalancer();
    List<HRegionInfo> regions = admin.getTableRegions(tableName);
    regions.addAll(admin.getTableRegions(TableName.META_TABLE_NAME));
    regions.addAll(admin.getTableRegions(TableName.NAMESPACE_TABLE_NAME));
    List<ServerName> servers = Lists.newArrayList(admin.getClusterStatus().getServers());
    Map<ServerName, List<HRegionInfo>> map = balancer.roundRobinAssignment(regions, servers);
    for (List<HRegionInfo> regionInfos : map.values()) {
      regions.removeAll(regionInfos);
    }
    assertEquals("No region should be missed by balancer", 0, regions.size());
  }


  @Test
  public void testBasicRegionPlacementAndReplicaLoad() throws Exception {

    String tableName = "testBasicRegionPlacement";
    HTableDescriptor desc = new HTableDescriptor(TableName.valueOf(tableName));
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc, Bytes.toBytes("aaa"), Bytes.toBytes("zzz"), REGION_NUM);
    TEST_UTIL.waitTableAvailable(desc.getTableName());

    FavoredNodesManager fnm = master.getFavoredNodesManager();
    List<HRegionInfo> regionsOfTable = admin.getTableRegions(TableName.valueOf(tableName));
    for (HRegionInfo rInfo : regionsOfTable) {
      Set<ServerName> favNodes = Sets.newHashSet(fnm.getFavoredNodes(rInfo));
      assertNotNull(favNodes);
      assertEquals(FavoredNodeAssignmentHelper.FAVORED_NODES_NUM, favNodes.size());
    }

    Map<ServerName, List<Integer>> replicaLoadMap =
        fnm.getReplicaLoad(Lists.newArrayList(admin.getClusterStatus().getServers()));
    assertTrue("Not all replica load collected.",
      admin.getClusterStatus().getServers().size() == replicaLoadMap.size());
    for (Entry<ServerName, List<Integer>> entry : replicaLoadMap.entrySet()) {
      assertTrue(entry.getValue().size() == FavoredNodeAssignmentHelper.FAVORED_NODES_NUM);
      assertTrue(entry.getValue().get(0) >= 0);
      assertTrue(entry.getValue().get(1) >= 0);
      assertTrue(entry.getValue().get(2) >= 0);
    }

    admin.disableTable(TableName.valueOf(tableName));
    admin.deleteTable(TableName.valueOf(tableName));
    replicaLoadMap =
        fnm.getReplicaLoad(Lists.newArrayList(admin.getClusterStatus().getServers()));
    assertTrue("replica load found " + replicaLoadMap.size() + " instead of 0.",
      replicaLoadMap.size() == admin.getClusterStatus().getServers().size());
  }

  @Test
  public void testRandomAssignmentWithNoFavNodes() throws Exception {

    final String tableName = "testRandomAssignmentWithNoFavNodes";
    HTableDescriptor desc = new HTableDescriptor(TableName.valueOf(tableName));
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc);
    TEST_UTIL.waitTableAvailable(desc.getTableName());

    HRegionInfo hri = admin.getTableRegions(TableName.valueOf(tableName)).get(0);

    FavoredNodesManager fnm = master.getFavoredNodesManager();
    fnm.deleteFavoredNodesForRegions(Lists.newArrayList(hri));
    assertNull("Favored nodes not found null after delete", fnm.getFavoredNodes(hri));

    LoadBalancer balancer = master.getLoadBalancer();
    ServerName destination = balancer.randomAssignment(hri, Lists.newArrayList(admin
        .getClusterStatus().getServers()));
    assertNotNull(destination);
    List<ServerName> favoredNodes = fnm.getFavoredNodes(hri);
    assertNotNull(favoredNodes);
    boolean containsFN = false;
    for (ServerName sn : favoredNodes) {
      if (ServerName.isSameHostnameAndPort(destination, sn)) {
        containsFN = true;
      }
    }
    assertTrue("Destination server does not belong to favored nodes.", containsFN);
  }

  @Test
  public void testBalancerWithoutFavoredNodes() throws Exception {

    TableName tableName = TableName.valueOf("testBalancerWithoutFavoredNodes");
    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc, Bytes.toBytes("aaa"), Bytes.toBytes("zzz"), REGION_NUM);
    TEST_UTIL.waitTableAvailable(tableName);

    final HRegionInfo region = admin.getTableRegions(tableName).get(0);
    LOG.info("Region thats supposed to be in transition: " + region);
    FavoredNodesManager fnm = master.getFavoredNodesManager();
    List<ServerName> currentFN = fnm.getFavoredNodes(region);
    assertNotNull(currentFN);

    fnm.deleteFavoredNodesForRegions(Lists.newArrayList(region));

    RegionStates regionStates = master.getAssignmentManager().getRegionStates();
    admin.setBalancerRunning(true, true);

    // Balancer should unassign the region
    assertTrue("Balancer did not run", admin.balancer());
    TEST_UTIL.waitUntilNoRegionsInTransition();

    admin.assign(region.getEncodedNameAsBytes());
    TEST_UTIL.waitUntilNoRegionsInTransition(60000);

    currentFN = fnm.getFavoredNodes(region);
    assertNotNull(currentFN);
    assertEquals("Expected number of FN not present",
      FavoredNodeAssignmentHelper.FAVORED_NODES_NUM, currentFN.size());

    assertTrue("Balancer did not run", admin.balancer());
    TEST_UTIL.waitUntilNoRegionsInTransition(60000);

    checkFavoredNodeAssignments(tableName, fnm, regionStates);
  }

  @Ignore @Test
  public void testMisplacedRegions() throws Exception {

    TableName tableName = TableName.valueOf("testMisplacedRegions");
    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc, Bytes.toBytes("aaa"), Bytes.toBytes("zzz"), REGION_NUM);
    TEST_UTIL.waitTableAvailable(tableName);

    final HRegionInfo misplacedRegion = admin.getTableRegions(tableName).get(0);
    FavoredNodesManager fnm = master.getFavoredNodesManager();
    List<ServerName> currentFN = fnm.getFavoredNodes(misplacedRegion);
    assertNotNull(currentFN);

    List<ServerName> serversForNewFN = Lists.newArrayList();
    for (ServerName sn : admin.getClusterStatus().getServers()) {
      serversForNewFN.add(ServerName.valueOf(sn.getHostname(), sn.getPort(), NON_STARTCODE));
    }
    for (ServerName sn : currentFN) {
      serversForNewFN.remove(sn);
    }
    FavoredNodeAssignmentHelper helper = new FavoredNodeAssignmentHelper(serversForNewFN, conf);
    helper.initialize();
    List<ServerName> newFavoredNodes = helper.generateFavoredNodes(misplacedRegion);
    assertNotNull(newFavoredNodes);
    assertEquals(FavoredNodeAssignmentHelper.FAVORED_NODES_NUM, newFavoredNodes.size());
    Map<HRegionInfo, List<ServerName>> regionFNMap = Maps.newHashMap();
    regionFNMap.put(misplacedRegion, newFavoredNodes);
    fnm.updateFavoredNodes(regionFNMap);

    final RegionStates regionStates = master.getAssignmentManager().getRegionStates();
    final ServerName current = regionStates.getRegionServerOfRegion(misplacedRegion);
    assertNull("Misplaced region is still hosted on favored node, not expected.",
        FavoredNodesPlan.getFavoredServerPosition(fnm.getFavoredNodes(misplacedRegion), current));
    admin.setBalancerRunning(true, true);
    assertTrue("Balancer did not run", admin.balancer());
    TEST_UTIL.waitFor(120000, 30000, new Waiter.Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        ServerName host = regionStates.getRegionServerOfRegion(misplacedRegion);
        return !ServerName.isSameHostnameAndPort(host, current);
      }
    });
    checkFavoredNodeAssignments(tableName, fnm, regionStates);
  }

  @Test
  public void test2FavoredNodesDead() throws Exception {

    TableName tableName = TableName.valueOf("testAllFavoredNodesDead");
    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc, Bytes.toBytes("aaa"), Bytes.toBytes("zzz"), REGION_NUM);
    TEST_UTIL.waitTableAvailable(tableName);

    final HRegionInfo region = admin.getTableRegions(tableName).get(0);
    LOG.info("Region that's supposed to be in transition: " + region);
    FavoredNodesManager fnm = master.getFavoredNodesManager();
    List<ServerName> currentFN = fnm.getFavoredNodes(region);
    assertNotNull(currentFN);

    List<ServerName> serversToStop = Lists.newArrayList(currentFN);
    serversToStop.remove(currentFN.get(0));

    // Lets kill 2 FN for the region. All regions should still be assigned
    stopServersAndWaitUntilProcessed(serversToStop);

    TEST_UTIL.waitUntilNoRegionsInTransition();
    final RegionStates regionStates = master.getAssignmentManager().getRegionStates();
    TEST_UTIL.waitFor(10000, new Waiter.Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        return regionStates.getRegionState(region).isOpened();
      }
    });

    assertEquals("Not all regions are online", REGION_NUM, admin.getTableRegions(tableName).size());
    admin.setBalancerRunning(true, true);
    assertTrue("Balancer did not run", admin.balancer());
    TEST_UTIL.waitUntilNoRegionsInTransition(60000);

    checkFavoredNodeAssignments(tableName, fnm, regionStates);
  }

  @Ignore @Test
  public void testAllFavoredNodesDead() throws Exception {

    TableName tableName = TableName.valueOf("testAllFavoredNodesDead");
    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc, Bytes.toBytes("aaa"), Bytes.toBytes("zzz"), REGION_NUM);
    TEST_UTIL.waitTableAvailable(tableName);

    final HRegionInfo region = admin.getTableRegions(tableName).get(0);
    LOG.info("Region that's supposed to be in transition: " + region);
    FavoredNodesManager fnm = master.getFavoredNodesManager();
    List<ServerName> currentFN = fnm.getFavoredNodes(region);
    assertNotNull(currentFN);

    // Lets kill all the RS that are favored nodes for this region.
    stopServersAndWaitUntilProcessed(currentFN);

    final RegionStates regionStates = master.getAssignmentManager().getRegionStates();
    TEST_UTIL.waitFor(10000, new Waiter.Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        return regionStates.getRegionState(region).isFailedOpen();
      }
    });

    assertTrue("Region: " + region + " should be RIT",
        regionStates.getRegionState(region).isFailedOpen());

    // Regenerate FN and assign, everything else should be fine
    List<ServerName> serversForNewFN = Lists.newArrayList();
    for (ServerName sn : admin.getClusterStatus().getServers()) {
      serversForNewFN.add(ServerName.valueOf(sn.getHostname(), sn.getPort(), NON_STARTCODE));
    }

    FavoredNodeAssignmentHelper helper = new FavoredNodeAssignmentHelper(serversForNewFN, conf);
    helper.initialize();

    for (RegionStateNode regionState: regionStates.getRegionsInTransition()) {
      HRegionInfo regionInfo = regionState.getRegionInfo();
      List<ServerName> newFavoredNodes = helper.generateFavoredNodes(regionInfo);
      assertNotNull(newFavoredNodes);
      assertEquals(FavoredNodeAssignmentHelper.FAVORED_NODES_NUM, newFavoredNodes.size());
      LOG.info("Region: " + regionInfo.getEncodedName() + " FN: " + newFavoredNodes);

      Map<HRegionInfo, List<ServerName>> regionFNMap = Maps.newHashMap();
      regionFNMap.put(regionInfo, newFavoredNodes);
      fnm.updateFavoredNodes(regionFNMap);
      LOG.info("Assigning region: " + regionInfo.getEncodedName());
      admin.assign(regionInfo.getEncodedNameAsBytes());
    }
    TEST_UTIL.waitUntilNoRegionsInTransition(60000);
    assertEquals("Not all regions are online", REGION_NUM, admin.getTableRegions(tableName).size());

    admin.setBalancerRunning(true, true);
    assertTrue("Balancer did not run", admin.balancer());
    TEST_UTIL.waitUntilNoRegionsInTransition(60000);

    checkFavoredNodeAssignments(tableName, fnm, regionStates);
  }

  @Ignore @Test
  public void testAllFavoredNodesDeadMasterRestarted() throws Exception {

    TableName tableName = TableName.valueOf("testAllFavoredNodesDeadMasterRestarted");
    HTableDescriptor desc = new HTableDescriptor(tableName);
    desc.addFamily(new HColumnDescriptor(HConstants.CATALOG_FAMILY));
    admin.createTable(desc, Bytes.toBytes("aaa"), Bytes.toBytes("zzz"), REGION_NUM);
    TEST_UTIL.waitTableAvailable(tableName);

    final HRegionInfo region = admin.getTableRegions(tableName).get(0);
    LOG.info("Region that's supposed to be in transition: " + region);
    FavoredNodesManager fnm = master.getFavoredNodesManager();
    List<ServerName> currentFN = fnm.getFavoredNodes(region);
    assertNotNull(currentFN);

    // Lets kill all the RS that are favored nodes for this region.
    stopServersAndWaitUntilProcessed(currentFN);

    final RegionStates regionStatesBeforeMaster =
        master.getAssignmentManager().getRegionStates();
    TEST_UTIL.waitFor(10000, new Waiter.Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        return regionStatesBeforeMaster.getRegionState(region).isFailedOpen();
      }
    });

    assertTrue("Region: " + region + " should be RIT",
        regionStatesBeforeMaster.getRegionState(region).isFailedOpen());

    List<HRegionInfo> rit = Lists.newArrayList();
    for (RegionStateNode regionState: regionStatesBeforeMaster.getRegionsInTransition()) {
      HRegionInfo regionInfo = regionState.getRegionInfo();
      LOG.debug("Region in transition after stopping FN's: " + regionInfo);
      rit.add(regionInfo);
      assertTrue("Region: " + regionInfo + " should be RIT",
          regionStatesBeforeMaster.getRegionState(regionInfo).isFailedOpen());
      assertEquals("Region: " + regionInfo + " does not belong to table: " + tableName,
          tableName, regionInfo.getTable());
    }

    Configuration conf = cluster.getConf();
    conf.setInt(ServerManager.WAIT_ON_REGIONSERVERS_MINTOSTART,
        SLAVES - FavoredNodeAssignmentHelper.FAVORED_NODES_NUM);

    cluster.stopMaster(master.getServerName());
    cluster.waitForMasterToStop(master.getServerName(), 60000);

    cluster.startMaster();
    cluster.waitForActiveAndReadyMaster();
    master = cluster.getMaster();
    fnm = master.getFavoredNodesManager();

    RegionStates regionStates = master.getAssignmentManager().getRegionStates();
    assertTrue("Region: " + region + " should be RIT",
        regionStates.getRegionState(region).isFailedOpen());

    for (HRegionInfo regionInfo : rit) {
      assertTrue("Region: " + regionInfo + " should be RIT",
          regionStates.getRegionState(regionInfo).isFailedOpen());
    }

    // Regenerate FN and assign, everything else should be fine
    List<ServerName> serversForNewFN = Lists.newArrayList();
    for (ServerName sn : admin.getClusterStatus().getServers()) {
      serversForNewFN.add(ServerName.valueOf(sn.getHostname(), sn.getPort(), NON_STARTCODE));
    }

    FavoredNodeAssignmentHelper helper = new FavoredNodeAssignmentHelper(serversForNewFN, conf);
    helper.initialize();

    for (HRegionInfo regionInfo : rit) {
      List<ServerName> newFavoredNodes = helper.generateFavoredNodes(regionInfo);
      assertNotNull(newFavoredNodes);
      assertEquals(FavoredNodeAssignmentHelper.FAVORED_NODES_NUM, newFavoredNodes.size());
      LOG.info("Region: " + regionInfo.getEncodedName() + " FN: " + newFavoredNodes);

      Map<HRegionInfo, List<ServerName>> regionFNMap = Maps.newHashMap();
      regionFNMap.put(regionInfo, newFavoredNodes);
      fnm.updateFavoredNodes(regionFNMap);
      LOG.info("Assigning region: " + regionInfo.getEncodedName());
      admin.assign(regionInfo.getEncodedNameAsBytes());
    }
    TEST_UTIL.waitUntilNoRegionsInTransition(60000);
    assertEquals("Not all regions are online", REGION_NUM, admin.getTableRegions(tableName).size());

    admin.setBalancerRunning(true, true);
    assertTrue("Balancer did not run", admin.balancer());
    TEST_UTIL.waitUntilNoRegionsInTransition(60000);

    checkFavoredNodeAssignments(tableName, fnm, regionStates);
  }

  private void checkFavoredNodeAssignments(TableName tableName, FavoredNodesManager fnm,
      RegionStates regionStates) throws IOException {
    for (HRegionInfo hri : admin.getTableRegions(tableName)) {
      ServerName host = regionStates.getRegionServerOfRegion(hri);
      assertNotNull("Region: " + hri.getEncodedName() + " not on FN, current: " + host
              + " FN list: " + fnm.getFavoredNodes(hri),
          FavoredNodesPlan.getFavoredServerPosition(fnm.getFavoredNodes(hri), host));
    }
  }

  private void stopServersAndWaitUntilProcessed(List<ServerName> currentFN) throws Exception {
    for (ServerName sn : currentFN) {
      for (JVMClusterUtil.RegionServerThread rst : cluster.getLiveRegionServerThreads()) {
        if (ServerName.isSameHostnameAndPort(sn, rst.getRegionServer().getServerName())) {
          LOG.info("Shutting down server: " + sn);
          cluster.stopRegionServer(rst.getRegionServer().getServerName());
          cluster.waitForRegionServerToStop(rst.getRegionServer().getServerName(), 60000);
        }
      }
    }

    // Wait until dead servers are processed.
    TEST_UTIL.waitFor(60000, new Waiter.Predicate<Exception>() {
      @Override
      public boolean evaluate() throws Exception {
        return !master.getServerManager().areDeadServersInProgress();
      }
    });

    assertEquals("Not all servers killed",
        SLAVES - currentFN.size(), cluster.getLiveRegionServerThreads().size());
  }

  private void compactTable(TableName tableName) throws IOException {
    for(JVMClusterUtil.RegionServerThread t : cluster.getRegionServerThreads()) {
      for(Region region : t.getRegionServer().getOnlineRegions(tableName)) {
        region.compact(true);
      }
    }
  }
}
