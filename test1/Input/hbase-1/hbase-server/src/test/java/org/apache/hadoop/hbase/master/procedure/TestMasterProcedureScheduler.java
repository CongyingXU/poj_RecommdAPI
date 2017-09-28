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

import static org.junit.Assert.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.master.locking.LockProcedure;
import org.apache.hadoop.hbase.procedure2.LockInfo;
import org.apache.hadoop.hbase.procedure2.Procedure;
import org.apache.hadoop.hbase.procedure2.ProcedureEvent;
import org.apache.hadoop.hbase.procedure2.LockInfo.WaitingProcedure;
import org.apache.hadoop.hbase.procedure2.ProcedureTestingUtility.TestProcedure;
import org.apache.hadoop.hbase.testclassification.MasterTests;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

@Category({MasterTests.class, SmallTests.class})
public class TestMasterProcedureScheduler {
  private static final Log LOG = LogFactory.getLog(TestMasterProcedureScheduler.class);

  private MasterProcedureScheduler queue;
  private Configuration conf;

  @Rule
  public TestName name = new TestName();

  @Before
  public void setUp() throws IOException {
    conf = HBaseConfiguration.create();
    queue = new MasterProcedureScheduler(conf);
    queue.start();
  }

  @After
  public void tearDown() throws IOException {
    assertEquals("proc-queue expected to be empty", 0, queue.size());
    queue.stop();
    queue.clear();
  }

  /**
   * Verify simple create/insert/fetch/delete of the table queue.
   */
  @Test
  public void testSimpleTableOpsQueues() throws Exception {
    final int NUM_TABLES = 10;
    final int NUM_ITEMS = 10;

    int count = 0;
    for (int i = 1; i <= NUM_TABLES; ++i) {
      TableName tableName = TableName.valueOf(String.format("test-%04d", i));
      // insert items
      for (int j = 1; j <= NUM_ITEMS; ++j) {
        queue.addBack(new TestTableProcedure(i * 1000 + j, tableName,
          TableProcedureInterface.TableOperationType.EDIT));
        assertEquals(++count, queue.size());
      }
    }
    assertEquals(NUM_TABLES * NUM_ITEMS, queue.size());

    for (int j = 1; j <= NUM_ITEMS; ++j) {
      for (int i = 1; i <= NUM_TABLES; ++i) {
        Procedure proc = queue.poll();
        assertTrue(proc != null);
        TableName tableName = ((TestTableProcedure)proc).getTableName();
        queue.waitTableExclusiveLock(proc, tableName);
        queue.wakeTableExclusiveLock(proc, tableName);
        queue.completionCleanup(proc);
        assertEquals(--count, queue.size());
        assertEquals(i * 1000 + j, proc.getProcId());
      }
    }
    assertEquals(0, queue.size());

    for (int i = 1; i <= NUM_TABLES; ++i) {
      final TableName tableName = TableName.valueOf(String.format("test-%04d", i));
      final TestTableProcedure dummyProc = new TestTableProcedure(100, tableName,
        TableProcedureInterface.TableOperationType.DELETE);
      // complete the table deletion
      assertTrue(queue.markTableAsDeleted(tableName, dummyProc));
    }
  }

  /**
   * Check that the table queue is not deletable until every procedure
   * in-progress is completed (this is a special case for write-locks).
   */
  @Test
  public void testCreateDeleteTableOperationsWithWriteLock() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());

    final TestTableProcedure dummyProc = new TestTableProcedure(100, tableName,
        TableProcedureInterface.TableOperationType.DELETE);

    queue.addBack(new TestTableProcedure(1, tableName,
          TableProcedureInterface.TableOperationType.EDIT));

    // table can't be deleted because one item is in the queue
    assertFalse(queue.markTableAsDeleted(tableName, dummyProc));

    // fetch item and take a lock
    Procedure proc = queue.poll();
    assertEquals(1, proc.getProcId());
    // take the xlock
    assertEquals(false, queue.waitTableExclusiveLock(proc, tableName));
    // table can't be deleted because we have the lock
    assertEquals(0, queue.size());
    assertFalse(queue.markTableAsDeleted(tableName, dummyProc));
    // release the xlock
    queue.wakeTableExclusiveLock(proc, tableName);
    // complete the table deletion
    assertTrue(queue.markTableAsDeleted(tableName, proc));
  }

  /**
   * Check that the table queue is not deletable until every procedure
   * in-progress is completed (this is a special case for read-locks).
   */
  @Test
  public void testCreateDeleteTableOperationsWithReadLock() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    final int nitems = 2;

    final TestTableProcedure dummyProc = new TestTableProcedure(100, tableName,
        TableProcedureInterface.TableOperationType.DELETE);

    for (int i = 1; i <= nitems; ++i) {
      queue.addBack(new TestTableProcedure(i, tableName,
            TableProcedureInterface.TableOperationType.READ));
    }

    // table can't be deleted because one item is in the queue
    assertFalse(queue.markTableAsDeleted(tableName, dummyProc));

    Procedure[] procs = new Procedure[nitems];
    for (int i = 0; i < nitems; ++i) {
      // fetch item and take a lock
      Procedure proc = procs[i] = queue.poll();
      assertEquals(i + 1, proc.getProcId());
      // take the rlock
      assertEquals(false, queue.waitTableSharedLock(proc, tableName));
      // table can't be deleted because we have locks and/or items in the queue
      assertFalse(queue.markTableAsDeleted(tableName, dummyProc));
    }

    for (int i = 0; i < nitems; ++i) {
      // table can't be deleted because we have locks
      assertFalse(queue.markTableAsDeleted(tableName, dummyProc));
      // release the rlock
      queue.wakeTableSharedLock(procs[i], tableName);
    }

    // there are no items and no lock in the queeu
    assertEquals(0, queue.size());
    // complete the table deletion
    assertTrue(queue.markTableAsDeleted(tableName, dummyProc));
  }

  /**
   * Verify the correct logic of RWLocks on the queue
   */
  @Test
  public void testVerifyRwLocks() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    queue.addBack(new TestTableProcedure(1, tableName,
          TableProcedureInterface.TableOperationType.EDIT));
    queue.addBack(new TestTableProcedure(2, tableName,
          TableProcedureInterface.TableOperationType.READ));
    queue.addBack(new TestTableProcedure(3, tableName,
          TableProcedureInterface.TableOperationType.EDIT));
    queue.addBack(new TestTableProcedure(4, tableName,
          TableProcedureInterface.TableOperationType.READ));
    queue.addBack(new TestTableProcedure(5, tableName,
          TableProcedureInterface.TableOperationType.READ));

    // Fetch the 1st item and take the write lock
    Procedure proc = queue.poll();
    assertEquals(1, proc.getProcId());
    assertEquals(false, queue.waitTableExclusiveLock(proc, tableName));

    // Fetch the 2nd item and verify that the lock can't be acquired
    assertEquals(null, queue.poll(0));

    // Release the write lock and acquire the read lock
    queue.wakeTableExclusiveLock(proc, tableName);

    // Fetch the 2nd item and take the read lock
    Procedure rdProc = queue.poll();
    assertEquals(2, rdProc.getProcId());
    assertEquals(false, queue.waitTableSharedLock(rdProc, tableName));

    // Fetch the 3rd item and verify that the lock can't be acquired
    assertEquals(null, queue.poll(0));

    // release the rdlock of item 2 and take the wrlock for the 3d item
    queue.wakeTableSharedLock(rdProc, tableName);

    // Fetch the 3rd item and take the write lock
    Procedure wrProc = queue.poll();
    assertEquals(false, queue.waitTableExclusiveLock(wrProc, tableName));

    // Fetch 4th item and verify that the lock can't be acquired
    assertEquals(null, queue.poll(0));

    // Release the write lock and acquire the read lock
    queue.wakeTableExclusiveLock(wrProc, tableName);

    // Fetch the 4th item and take the read lock
    rdProc = queue.poll();
    assertEquals(4, rdProc.getProcId());
    assertEquals(false, queue.waitTableSharedLock(rdProc, tableName));

    // Fetch the 4th item and take the read lock
    Procedure rdProc2 = queue.poll();
    assertEquals(5, rdProc2.getProcId());
    assertEquals(false, queue.waitTableSharedLock(rdProc2, tableName));

    // Release 4th and 5th read-lock
    queue.wakeTableSharedLock(rdProc, tableName);
    queue.wakeTableSharedLock(rdProc2, tableName);

    // remove table queue
    assertEquals(0, queue.size());
    assertTrue("queue should be deleted", queue.markTableAsDeleted(tableName, wrProc));
  }

  @Test
  public void testVerifyNamespaceRwLocks() throws Exception {
    String nsName1 = "ns1";
    String nsName2 = "ns2";
    TableName tableName1 = TableName.valueOf(nsName1, name.getMethodName());
    TableName tableName2 = TableName.valueOf(nsName2, name.getMethodName());
    queue.addBack(new TestNamespaceProcedure(1, nsName1,
          TableProcedureInterface.TableOperationType.EDIT));
    queue.addBack(new TestTableProcedure(2, tableName1,
          TableProcedureInterface.TableOperationType.EDIT));
    queue.addBack(new TestTableProcedure(3, tableName2,
          TableProcedureInterface.TableOperationType.EDIT));
    queue.addBack(new TestNamespaceProcedure(4, nsName2,
          TableProcedureInterface.TableOperationType.EDIT));

    // Fetch the 1st item and take the write lock
    Procedure procNs1 = queue.poll();
    assertEquals(1, procNs1.getProcId());
    assertEquals(false, queue.waitNamespaceExclusiveLock(procNs1, nsName1));

    // System tables have 2 as default priority
    Procedure procNs2 = queue.poll();
    assertEquals(4, procNs2.getProcId());
    assertEquals(false, queue.waitNamespaceExclusiveLock(procNs2, nsName2));
    queue.wakeNamespaceExclusiveLock(procNs2, nsName2);

    // add procNs2 back in the queue
    queue.yield(procNs2);

    // table on ns1 is locked, so we get table on ns2
    procNs2 = queue.poll();
    assertEquals(3, procNs2.getProcId());
    assertEquals(false, queue.waitTableExclusiveLock(procNs2, tableName2));

    // ns2 is not available (TODO we may avoid this one)
    Procedure procNs2b = queue.poll();
    assertEquals(4, procNs2b.getProcId());
    assertEquals(true, queue.waitNamespaceExclusiveLock(procNs2b, nsName2));

    // release the ns1 lock
    queue.wakeNamespaceExclusiveLock(procNs1, nsName1);

    // we are now able to execute table of ns1
    long procId = queue.poll().getProcId();
    assertEquals(2, procId);

    // release ns2
    queue.wakeTableExclusiveLock(procNs2, tableName2);

    // we are now able to execute ns2
    procId = queue.poll().getProcId();
    assertEquals(4, procId);
  }

  @Test
  public void testVerifyNamespaceXLock() throws Exception {
    String nsName = "ns1";
    TableName tableName = TableName.valueOf(nsName, name.getMethodName());
    queue.addBack(new TestNamespaceProcedure(1, nsName,
          TableProcedureInterface.TableOperationType.CREATE));
    queue.addBack(new TestTableProcedure(2, tableName,
          TableProcedureInterface.TableOperationType.READ));

    // Fetch the ns item and take the xlock
    Procedure proc = queue.poll();
    assertEquals(1, proc.getProcId());
    assertEquals(false, queue.waitNamespaceExclusiveLock(proc, nsName));

    // the table operation can't be executed because the ns is locked
    assertEquals(null, queue.poll(0));

    // release the ns lock
    queue.wakeNamespaceExclusiveLock(proc, nsName);

    proc = queue.poll();
    assertEquals(2, proc.getProcId());
    assertEquals(false, queue.waitTableExclusiveLock(proc, tableName));
    queue.wakeTableExclusiveLock(proc, tableName);
  }

  @Test
  public void testXLockWaitingForExecutingSharedLockToRelease() {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    final HRegionInfo regionA = new HRegionInfo(tableName, Bytes.toBytes("a"), Bytes.toBytes("b"));

    queue.addBack(new TestRegionProcedure(1, tableName,
        TableProcedureInterface.TableOperationType.REGION_ASSIGN, regionA));
    queue.addBack(new TestTableProcedure(2, tableName,
          TableProcedureInterface.TableOperationType.EDIT));
    queue.addBack(new TestRegionProcedure(3, tableName,
        TableProcedureInterface.TableOperationType.REGION_UNASSIGN, regionA));

    // Fetch the 1st item and take the shared lock
    Procedure proc = queue.poll();
    assertEquals(1, proc.getProcId());
    assertEquals(false, queue.waitRegion(proc, regionA));

    // the xlock operation in the queue can't be executed
    assertEquals(null, queue.poll(0));

    // release the shared lock
    queue.wakeRegion(proc, regionA);

    // Fetch the 2nd item and take the xlock
    proc = queue.poll();
    assertEquals(2, proc.getProcId());
    assertEquals(false, queue.waitTableExclusiveLock(proc, tableName));

    // everything is locked by the table operation
    assertEquals(null, queue.poll(0));

    // release the table xlock
    queue.wakeTableExclusiveLock(proc, tableName);

    // grab the last item in the queue
    proc = queue.poll();
    assertEquals(3, proc.getProcId());

    // lock and unlock the region
    assertEquals(false, queue.waitRegion(proc, regionA));
    assertEquals(null, queue.poll(0));
    queue.wakeRegion(proc, regionA);
  }

  @Test
  public void testVerifyRegionLocks() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    final HRegionInfo regionA = new HRegionInfo(tableName, Bytes.toBytes("a"), Bytes.toBytes("b"));
    final HRegionInfo regionB = new HRegionInfo(tableName, Bytes.toBytes("b"), Bytes.toBytes("c"));
    final HRegionInfo regionC = new HRegionInfo(tableName, Bytes.toBytes("c"), Bytes.toBytes("d"));

    queue.addBack(new TestTableProcedure(1, tableName,
          TableProcedureInterface.TableOperationType.EDIT));
    queue.addBack(new TestRegionProcedure(2, tableName,
        TableProcedureInterface.TableOperationType.REGION_MERGE, regionA, regionB));
    queue.addBack(new TestRegionProcedure(3, tableName,
        TableProcedureInterface.TableOperationType.REGION_SPLIT, regionA));
    queue.addBack(new TestRegionProcedure(4, tableName,
        TableProcedureInterface.TableOperationType.REGION_SPLIT, regionB));
    queue.addBack(new TestRegionProcedure(5, tableName,
        TableProcedureInterface.TableOperationType.REGION_UNASSIGN, regionC));

    // Fetch the 1st item and take the write lock
    Procedure proc = queue.poll();
    assertEquals(1, proc.getProcId());
    assertEquals(false, queue.waitTableExclusiveLock(proc, tableName));

    // everything is locked by the table operation
    assertEquals(null, queue.poll(0));

    // release the table lock
    queue.wakeTableExclusiveLock(proc, tableName);

    // Fetch the 2nd item and the the lock on regionA and regionB
    Procedure mergeProc = queue.poll();
    assertEquals(2, mergeProc.getProcId());
    assertEquals(false, queue.waitRegions(mergeProc, tableName, regionA, regionB));

    // Fetch the 3rd item and the try to lock region A which will fail
    // because already locked. this procedure will go in waiting.
    // (this stuff will be explicit until we get rid of the zk-lock)
    Procedure procA = queue.poll();
    assertEquals(3, procA.getProcId());
    assertEquals(true, queue.waitRegions(procA, tableName, regionA));

    // Fetch the 4th item, same story as the 3rd
    Procedure procB = queue.poll();
    assertEquals(4, procB.getProcId());
    assertEquals(true, queue.waitRegions(procB, tableName, regionB));

    // Fetch the 5th item, since it is a non-locked region we are able to execute it
    Procedure procC = queue.poll();
    assertEquals(5, procC.getProcId());
    assertEquals(false, queue.waitRegions(procC, tableName, regionC));

    // 3rd and 4th are in the region suspended queue
    assertEquals(null, queue.poll(0));

    // Release region A-B from merge operation (procId=2)
    queue.wakeRegions(mergeProc, tableName, regionA, regionB);

    // Fetch the 3rd item, now the lock on the region is available
    procA = queue.poll();
    assertEquals(3, procA.getProcId());
    assertEquals(false, queue.waitRegions(procA, tableName, regionA));

    // Fetch the 4th item, now the lock on the region is available
    procB = queue.poll();
    assertEquals(4, procB.getProcId());
    assertEquals(false, queue.waitRegions(procB, tableName, regionB));

    // release the locks on the regions
    queue.wakeRegions(procA, tableName, regionA);
    queue.wakeRegions(procB, tableName, regionB);
    queue.wakeRegions(procC, tableName, regionC);
  }

  @Test
  public void testVerifySubProcRegionLocks() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    final HRegionInfo regionA = new HRegionInfo(tableName, Bytes.toBytes("a"), Bytes.toBytes("b"));
    final HRegionInfo regionB = new HRegionInfo(tableName, Bytes.toBytes("b"), Bytes.toBytes("c"));
    final HRegionInfo regionC = new HRegionInfo(tableName, Bytes.toBytes("c"), Bytes.toBytes("d"));

    queue.addBack(new TestTableProcedure(1, tableName,
        TableProcedureInterface.TableOperationType.ENABLE));

    // Fetch the 1st item from the queue, "the root procedure" and take the table lock
    Procedure rootProc = queue.poll();
    assertEquals(1, rootProc.getProcId());
    assertEquals(false, queue.waitTableExclusiveLock(rootProc, tableName));
    assertEquals(null, queue.poll(0));

    // Execute the 1st step of the root-proc.
    // we should get 3 sub-proc back, one for each region.
    // (this step is done by the executor/rootProc, we are simulating it)
    Procedure[] subProcs = new Procedure[] {
      new TestRegionProcedure(1, 2, tableName,
        TableProcedureInterface.TableOperationType.REGION_EDIT, regionA),
      new TestRegionProcedure(1, 3, tableName,
        TableProcedureInterface.TableOperationType.REGION_EDIT, regionB),
      new TestRegionProcedure(1, 4, tableName,
        TableProcedureInterface.TableOperationType.REGION_EDIT, regionC),
    };

    // at this point the rootProc is going in a waiting state
    // and the sub-procedures will be added in the queue.
    // (this step is done by the executor, we are simulating it)
    for (int i = subProcs.length - 1; i >= 0; --i) {
      queue.addFront(subProcs[i]);
    }
    assertEquals(subProcs.length, queue.size());

    // we should be able to fetch and execute all the sub-procs,
    // since they are operating on different regions
    for (int i = 0; i < subProcs.length; ++i) {
      TestRegionProcedure regionProc = (TestRegionProcedure)queue.poll(0);
      assertEquals(subProcs[i].getProcId(), regionProc.getProcId());
      assertEquals(false, queue.waitRegions(regionProc, tableName, regionProc.getRegionInfo()));
    }

    // nothing else in the queue
    assertEquals(null, queue.poll(0));

    // release all the region locks
    for (int i = 0; i < subProcs.length; ++i) {
      TestRegionProcedure regionProc = (TestRegionProcedure)subProcs[i];
      queue.wakeRegions(regionProc, tableName, regionProc.getRegionInfo());
    }

    // nothing else in the queue
    assertEquals(null, queue.poll(0));

    // release the table lock (for the root procedure)
    queue.wakeTableExclusiveLock(rootProc, tableName);
  }

  @Test
  public void testInheritedRegionXLock() {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    final HRegionInfo region = new HRegionInfo(tableName, Bytes.toBytes("a"), Bytes.toBytes("b"));

    queue.addBack(new TestRegionProcedure(1, tableName,
        TableProcedureInterface.TableOperationType.REGION_SPLIT, region));
    queue.addBack(new TestRegionProcedure(1, 2, tableName,
        TableProcedureInterface.TableOperationType.REGION_UNASSIGN, region));
    queue.addBack(new TestRegionProcedure(3, tableName,
        TableProcedureInterface.TableOperationType.REGION_EDIT, region));

    // fetch the root proc and take the lock on the region
    Procedure rootProc = queue.poll();
    assertEquals(1, rootProc.getProcId());
    assertEquals(false, queue.waitRegion(rootProc, region));

    // fetch the sub-proc and take the lock on the region (inherited lock)
    Procedure childProc = queue.poll();
    assertEquals(2, childProc.getProcId());
    assertEquals(false, queue.waitRegion(childProc, region));

    // proc-3 will be fetched but it can't take the lock
    Procedure proc = queue.poll();
    assertEquals(3, proc.getProcId());
    assertEquals(true, queue.waitRegion(proc, region));

    // release the child lock
    queue.wakeRegion(childProc, region);

    // nothing in the queue (proc-3 is suspended)
    assertEquals(null, queue.poll(0));

    // release the root lock
    queue.wakeRegion(rootProc, region);

    // proc-3 should be now available
    proc = queue.poll();
    assertEquals(3, proc.getProcId());
    assertEquals(false, queue.waitRegion(proc, region));
    queue.wakeRegion(proc, region);
  }

  @Test
  public void testSuspendedProcedure() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());

    queue.addBack(new TestTableProcedure(1, tableName,
        TableProcedureInterface.TableOperationType.READ));
    queue.addBack(new TestTableProcedure(2, tableName,
        TableProcedureInterface.TableOperationType.READ));

    Procedure proc = queue.poll();
    assertEquals(1, proc.getProcId());

    // suspend
    ProcedureEvent event = new ProcedureEvent("testSuspendedProcedureEvent");
    assertEquals(true, queue.waitEvent(event, proc));

    proc = queue.poll();
    assertEquals(2, proc.getProcId());
    assertEquals(null, queue.poll(0));

    // resume
    queue.wakeEvent(event);

    proc = queue.poll();
    assertEquals(1, proc.getProcId());
    assertEquals(null, queue.poll(0));
  }

  private static HRegionInfo[] generateRegionInfo(final TableName tableName) {
    return new HRegionInfo[] {
      new HRegionInfo(tableName, Bytes.toBytes("a"), Bytes.toBytes("b")),
      new HRegionInfo(tableName, Bytes.toBytes("b"), Bytes.toBytes("c")),
      new HRegionInfo(tableName, Bytes.toBytes("c"), Bytes.toBytes("d")),
    };
  }

  @Test
  public void testParentXLockAndChildrenSharedLock() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    final HRegionInfo[] regions = generateRegionInfo(tableName);
    final TestRegionProcedure[] childProcs = new TestRegionProcedure[regions.length];
    for (int i = 0; i < regions.length; ++i) {
      childProcs[i] = new TestRegionProcedure(1, 2 + i, tableName,
          TableProcedureInterface.TableOperationType.REGION_ASSIGN, regions[i]);
    }
    testInheritedXLockAndChildrenSharedLock(tableName,
      new TestTableProcedure(1, tableName, TableProcedureInterface.TableOperationType.CREATE),
      childProcs
    );
  }

  @Test
  public void testRootXLockAndChildrenSharedLock() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    final HRegionInfo[] regions = generateRegionInfo(tableName);
    final TestRegionProcedure[] childProcs = new TestRegionProcedure[regions.length];
    for (int i = 0; i < regions.length; ++i) {
      childProcs[i] = new TestRegionProcedure(1, 2, 3 + i, tableName,
          TableProcedureInterface.TableOperationType.REGION_ASSIGN, regions[i]);
    }
    testInheritedXLockAndChildrenSharedLock(tableName,
      new TestTableProcedure(1, tableName, TableProcedureInterface.TableOperationType.CREATE),
      childProcs
    );
  }

  private void testInheritedXLockAndChildrenSharedLock(final TableName tableName,
      final TestTableProcedure rootProc, final TestRegionProcedure[] childProcs)
      throws Exception {
    queue.addBack(rootProc);

    // fetch and acquire first xlock proc
    Procedure parentProc = queue.poll();
    assertEquals(rootProc, parentProc);
    assertEquals(false, queue.waitTableExclusiveLock(parentProc, tableName));

    // add child procedure
    for (int i = 0; i < childProcs.length; ++i) {
      queue.addFront(childProcs[i]);
    }

    // add another xlock procedure (no parent)
    queue.addBack(new TestTableProcedure(100, tableName,
        TableProcedureInterface.TableOperationType.EDIT));

    // fetch and execute child
    for (int i = 0; i < childProcs.length; ++i) {
      TestRegionProcedure childProc = (TestRegionProcedure)queue.poll();
      LOG.debug("fetch children " + childProc);
      assertEquals(false, queue.waitRegions(childProc, tableName, childProc.getRegionInfo()));
      queue.wakeRegions(childProc, tableName, childProc.getRegionInfo());
    }

    // nothing available, until xlock release
    assertEquals(null, queue.poll(0));

    // release xlock
    queue.wakeTableExclusiveLock(parentProc, tableName);

    // fetch the other xlock proc
    Procedure proc = queue.poll();
    assertEquals(100, proc.getProcId());
    assertEquals(false, queue.waitTableExclusiveLock(proc, tableName));
    queue.wakeTableExclusiveLock(proc, tableName);
  }

  @Test
  public void testParentXLockAndChildrenXLock() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    testInheritedXLockAndChildrenXLock(tableName,
      new TestTableProcedure(1, tableName, TableProcedureInterface.TableOperationType.EDIT),
      new TestTableProcedure(1, 2, tableName, TableProcedureInterface.TableOperationType.EDIT)
    );
  }

  @Test
  public void testRootXLockAndChildrenXLock() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());
    // simulate 3 procedures: 1 (root), (2) child of root, (3) child of proc-2
    testInheritedXLockAndChildrenXLock(tableName,
      new TestTableProcedure(1, tableName, TableProcedureInterface.TableOperationType.EDIT),
      new TestTableProcedure(1, 2, 3, tableName, TableProcedureInterface.TableOperationType.EDIT)
    );
  }

  private void testInheritedXLockAndChildrenXLock(final TableName tableName,
      final TestTableProcedure rootProc, final TestTableProcedure childProc) throws Exception {
    queue.addBack(rootProc);

    // fetch and acquire first xlock proc
    Procedure parentProc = queue.poll();
    assertEquals(rootProc, parentProc);
    assertEquals(false, queue.waitTableExclusiveLock(parentProc, tableName));

    // add child procedure
    queue.addFront(childProc);

    // fetch the other xlock proc
    Procedure proc = queue.poll();
    assertEquals(childProc, proc);
    assertEquals(false, queue.waitTableExclusiveLock(proc, tableName));
    queue.wakeTableExclusiveLock(proc, tableName);

    // release xlock
    queue.wakeTableExclusiveLock(parentProc, tableName);
  }

  @Test
  public void testYieldWithXLockHeld() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());

    queue.addBack(new TestTableProcedure(1, tableName,
        TableProcedureInterface.TableOperationType.EDIT));
    queue.addBack(new TestTableProcedure(2, tableName,
        TableProcedureInterface.TableOperationType.EDIT));

    // fetch from the queue and acquire xlock for the first proc
    Procedure proc = queue.poll();
    assertEquals(1, proc.getProcId());
    assertEquals(false, queue.waitTableExclusiveLock(proc, tableName));

    // nothing available, until xlock release
    assertEquals(null, queue.poll(0));

    // put the proc in the queue
    queue.yield(proc);

    // fetch from the queue, it should be the one with just added back
    proc = queue.poll();
    assertEquals(1, proc.getProcId());

    // release the xlock
    queue.wakeTableExclusiveLock(proc, tableName);

    proc = queue.poll();
    assertEquals(2, proc.getProcId());
  }

  @Test
  public void testYieldWithSharedLockHeld() throws Exception {
    final TableName tableName = TableName.valueOf(name.getMethodName());

    queue.addBack(new TestTableProcedure(1, tableName,
        TableProcedureInterface.TableOperationType.READ));
    queue.addBack(new TestTableProcedure(2, tableName,
        TableProcedureInterface.TableOperationType.READ));
    queue.addBack(new TestTableProcedure(3, tableName,
        TableProcedureInterface.TableOperationType.EDIT));

    // fetch and acquire the first shared-lock
    Procedure proc1 = queue.poll();
    assertEquals(1, proc1.getProcId());
    assertEquals(false, queue.waitTableSharedLock(proc1, tableName));

    // fetch and acquire the second shared-lock
    Procedure proc2 = queue.poll();
    assertEquals(2, proc2.getProcId());
    assertEquals(false, queue.waitTableSharedLock(proc2, tableName));

    // nothing available, until xlock release
    assertEquals(null, queue.poll(0));

    // put the procs back in the queue
    queue.yield(proc2);
    queue.yield(proc1);

    // fetch from the queue, it should fetch the ones with just added back
    proc1 = queue.poll();
    assertEquals(1, proc1.getProcId());
    proc2 = queue.poll();
    assertEquals(2, proc2.getProcId());

    // release the xlock
    queue.wakeTableSharedLock(proc1, tableName);
    queue.wakeTableSharedLock(proc2, tableName);

    Procedure proc3 = queue.poll();
    assertEquals(3, proc3.getProcId());
  }

  public static class TestTableProcedure extends TestProcedure
      implements TableProcedureInterface {
    private final TableOperationType opType;
    private final TableName tableName;

    public TestTableProcedure() {
      throw new UnsupportedOperationException("recovery should not be triggered here");
    }

    public TestTableProcedure(long procId, TableName tableName, TableOperationType opType) {
      this(-1, procId, tableName, opType);
    }

    public TestTableProcedure(long parentProcId, long procId, TableName tableName,
        TableOperationType opType) {
      this(-1, parentProcId, procId, tableName, opType);
    }

    public TestTableProcedure(long rootProcId, long parentProcId, long procId, TableName tableName,
        TableOperationType opType) {
      super(procId, parentProcId, rootProcId, null);
      this.tableName = tableName;
      this.opType = opType;
    }
    @Override
    public TableName getTableName() {
      return tableName;
    }

    @Override
    public TableOperationType getTableOperationType() {
      return opType;
    }

    @Override
    public void toStringClassDetails(final StringBuilder sb) {
      sb.append(getClass().getSimpleName());
      sb.append("(table=");
      sb.append(getTableName());
      sb.append(")");
    }
  }

  public static class TestTableProcedureWithEvent extends TestTableProcedure {
    private final ProcedureEvent event;

    public TestTableProcedureWithEvent(long procId, TableName tableName, TableOperationType opType) {
      super(procId, tableName, opType);
      event = new ProcedureEvent(tableName + " procId=" + procId);
    }

    public ProcedureEvent getEvent() {
      return event;
    }
  }

  public static class TestRegionProcedure extends TestTableProcedure {
    private final HRegionInfo[] regionInfo;

    public TestRegionProcedure() {
      throw new UnsupportedOperationException("recovery should not be triggered here");
    }

    public TestRegionProcedure(long procId, TableName tableName, TableOperationType opType,
        HRegionInfo... regionInfo) {
      this(-1, procId, tableName, opType, regionInfo);
    }

    public TestRegionProcedure(long parentProcId, long procId, TableName tableName,
        TableOperationType opType, HRegionInfo... regionInfo) {
      this(-1, parentProcId, procId, tableName, opType, regionInfo);
    }

    public TestRegionProcedure(long rootProcId, long parentProcId, long procId, TableName tableName,
        TableOperationType opType, HRegionInfo... regionInfo) {
      super(rootProcId, parentProcId, procId, tableName, opType);
      this.regionInfo = regionInfo;
    }

    public HRegionInfo[] getRegionInfo() {
      return regionInfo;
    }

    @Override
    public void toStringClassDetails(final StringBuilder sb) {
      sb.append(getClass().getSimpleName());
      sb.append("(regions=");
      sb.append(Arrays.toString(getRegionInfo()));
      sb.append(")");
    }
  }

  public static class TestNamespaceProcedure extends TestProcedure
      implements TableProcedureInterface {
    private final TableOperationType opType;
    private final String nsName;

    public TestNamespaceProcedure() {
      throw new UnsupportedOperationException("recovery should not be triggered here");
    }

    public TestNamespaceProcedure(long procId, String nsName, TableOperationType opType) {
      super(procId);
      this.nsName = nsName;
      this.opType = opType;
    }

    @Override
    public TableName getTableName() {
      return TableName.NAMESPACE_TABLE_NAME;
    }

    @Override
    public TableOperationType getTableOperationType() {
      return opType;
    }

    @Override
    public void toStringClassDetails(final StringBuilder sb) {
      sb.append(getClass().getSimpleName());
      sb.append("(ns=");
      sb.append(nsName);
      sb.append(")");
    }
  }

  private static LockProcedure createLockProcedure(LockProcedure.LockType lockType, long procId) throws Exception {
    LockProcedure procedure = new LockProcedure();

    Field typeField = LockProcedure.class.getDeclaredField("type");
    typeField.setAccessible(true);
    typeField.set(procedure, lockType);

    Method setProcIdMethod = Procedure.class.getDeclaredMethod("setProcId", long.class);
    setProcIdMethod.setAccessible(true);
    setProcIdMethod.invoke(procedure, procId);

    return procedure;
  }

  private static LockProcedure createExclusiveLockProcedure(long procId) throws Exception {
    return createLockProcedure(LockProcedure.LockType.EXCLUSIVE, procId);
  }

  private static LockProcedure createSharedLockProcedure(long procId) throws Exception {
    return createLockProcedure(LockProcedure.LockType.SHARED, procId);
  }

  private static void assertLockResource(LockInfo lock,
      LockInfo.ResourceType resourceType, String resourceName)
  {
    assertEquals(resourceType, lock.getResourceType());
    assertEquals(resourceName, lock.getResourceName());
  }

  private static void assertExclusiveLock(LockInfo lock, long procId)
  {
    assertEquals(LockInfo.LockType.EXCLUSIVE, lock.getLockType());
    assertEquals(procId, lock.getExclusiveLockOwnerProcedure().getProcId());
    assertEquals(0, lock.getSharedLockCount());
  }

  private static void assertSharedLock(LockInfo lock, int lockCount)
  {
    assertEquals(LockInfo.LockType.SHARED, lock.getLockType());
    assertEquals(lockCount, lock.getSharedLockCount());
  }

  @Test
  public void testListLocksServer() throws Exception {
    LockProcedure procedure = createExclusiveLockProcedure(0);
    queue.waitServerExclusiveLock(procedure, ServerName.valueOf("server1,1234,0"));

    List<LockInfo> locks = queue.listLocks();
    assertEquals(1, locks.size());

    LockInfo serverLock = locks.get(0);
    assertLockResource(serverLock, LockInfo.ResourceType.SERVER, "server1,1234,0");
    assertExclusiveLock(serverLock, 0);
    assertTrue(serverLock.getWaitingProcedures().isEmpty());
  }

  @Test
  public void testListLocksNamespace() throws Exception {
    LockProcedure procedure = createExclusiveLockProcedure(1);
    queue.waitNamespaceExclusiveLock(procedure, "ns1");

    List<LockInfo> locks = queue.listLocks();
    assertEquals(2, locks.size());

    LockInfo namespaceLock = locks.get(0);
    assertLockResource(namespaceLock, LockInfo.ResourceType.NAMESPACE, "ns1");
    assertExclusiveLock(namespaceLock, 1);
    assertTrue(namespaceLock.getWaitingProcedures().isEmpty());

    LockInfo tableLock = locks.get(1);
    assertLockResource(tableLock, LockInfo.ResourceType.TABLE,
        TableName.NAMESPACE_TABLE_NAME.getNameAsString());
    assertSharedLock(tableLock, 1);
    assertTrue(tableLock.getWaitingProcedures().isEmpty());
  }

  @Test
  public void testListLocksTable() throws Exception {
    LockProcedure procedure = createExclusiveLockProcedure(2);
    queue.waitTableExclusiveLock(procedure, TableName.valueOf("ns2", "table2"));

    List<LockInfo> locks = queue.listLocks();
    assertEquals(2, locks.size());

    LockInfo namespaceLock = locks.get(0);
    assertLockResource(namespaceLock, LockInfo.ResourceType.NAMESPACE, "ns2");
    assertSharedLock(namespaceLock, 1);
    assertTrue(namespaceLock.getWaitingProcedures().isEmpty());

    LockInfo tableLock = locks.get(1);
    assertLockResource(tableLock, LockInfo.ResourceType.TABLE, "ns2:table2");
    assertExclusiveLock(tableLock, 2);
    assertTrue(tableLock.getWaitingProcedures().isEmpty());
  }

  @Test
  public void testListLocksRegion() throws Exception {
    LockProcedure procedure = createExclusiveLockProcedure(3);
    HRegionInfo regionInfo = new HRegionInfo(TableName.valueOf("ns3", "table3"));

    queue.waitRegion(procedure, regionInfo);

    List<LockInfo> locks = queue.listLocks();
    assertEquals(3, locks.size());

    LockInfo namespaceLock = locks.get(0);
    assertLockResource(namespaceLock, LockInfo.ResourceType.NAMESPACE, "ns3");
    assertSharedLock(namespaceLock, 1);
    assertTrue(namespaceLock.getWaitingProcedures().isEmpty());

    LockInfo tableLock = locks.get(1);
    assertLockResource(tableLock, LockInfo.ResourceType.TABLE, "ns3:table3");
    assertSharedLock(tableLock, 1);
    assertTrue(tableLock.getWaitingProcedures().isEmpty());

    LockInfo regionLock = locks.get(2);
    assertLockResource(regionLock, LockInfo.ResourceType.REGION, regionInfo.getEncodedName());
    assertExclusiveLock(regionLock, 3);
    assertTrue(regionLock.getWaitingProcedures().isEmpty());
  }

  @Test
  public void testListLocksWaiting() throws Exception {
    LockProcedure procedure1 = createExclusiveLockProcedure(1);
    queue.waitTableExclusiveLock(procedure1, TableName.valueOf("ns4", "table4"));

    LockProcedure procedure2 = createSharedLockProcedure(2);
    queue.waitTableSharedLock(procedure2, TableName.valueOf("ns4", "table4"));

    LockProcedure procedure3 = createExclusiveLockProcedure(3);
    queue.waitTableExclusiveLock(procedure3, TableName.valueOf("ns4", "table4"));

    List<LockInfo> locks = queue.listLocks();
    assertEquals(2, locks.size());

    LockInfo namespaceLock = locks.get(0);
    assertLockResource(namespaceLock, LockInfo.ResourceType.NAMESPACE, "ns4");
    assertSharedLock(namespaceLock, 1);
    assertTrue(namespaceLock.getWaitingProcedures().isEmpty());

    LockInfo tableLock = locks.get(1);
    assertLockResource(tableLock, LockInfo.ResourceType.TABLE, "ns4:table4");
    assertExclusiveLock(tableLock, 1);

    List<WaitingProcedure> waitingProcedures = tableLock.getWaitingProcedures();
    assertEquals(2, waitingProcedures.size());

    WaitingProcedure waitingProcedure1 = waitingProcedures.get(0);
    assertEquals(LockInfo.LockType.SHARED, waitingProcedure1.getLockType());
    assertEquals(2, waitingProcedure1.getProcedure().getProcId());

    WaitingProcedure waitingProcedure2 = waitingProcedures.get(1);
    assertEquals(LockInfo.LockType.EXCLUSIVE, waitingProcedure2.getLockType());
    assertEquals(3, waitingProcedure2.getProcedure().getProcId());
  }
}

