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

import static org.apache.hadoop.hbase.coprocessor.CoprocessorHost.MASTER_COPROCESSOR_CONF_KEY;
import static org.apache.hadoop.hbase.quotas.MasterSpaceQuotaObserver.REMOVE_QUOTA_ON_TABLE_DELETE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.master.HMaster;
import org.apache.hadoop.hbase.security.access.AccessController;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test class for MasterSpaceQuotaObserver that does not require a cluster.
 */
@Category(SmallTests.class)
public class TestMasterSpaceQuotaObserverWithMocks {

  private HMaster master;
  private Configuration conf;

  @Before
  public void setup() {
    conf = HBaseConfiguration.create();
    master = mock(HMaster.class);
    doCallRealMethod().when(master).updateConfigurationForSpaceQuotaObserver(
        any(Configuration.class));
  }

  @Test
  public void testAddDefaultObserver() {
    master.updateConfigurationForSpaceQuotaObserver(conf);
    assertEquals(MasterSpaceQuotaObserver.class.getName(), conf.get(MASTER_COPROCESSOR_CONF_KEY));
  }

  @Test
  public void testDoNotAddDefaultObserver() {
    conf.setBoolean(REMOVE_QUOTA_ON_TABLE_DELETE, false);
    master.updateConfigurationForSpaceQuotaObserver(conf);
    // Configuration#getStrings returns null when unset
    assertNull(conf.getStrings(MASTER_COPROCESSOR_CONF_KEY));
  }

  @Test
  public void testAppendsObserver() {
    conf.set(MASTER_COPROCESSOR_CONF_KEY, AccessController.class.getName());
    master.updateConfigurationForSpaceQuotaObserver(conf);
    Set<String> coprocs = new HashSet<>(conf.getStringCollection(MASTER_COPROCESSOR_CONF_KEY));
    assertEquals(2, coprocs.size());
    assertTrue(
        "Observed coprocessors were: " + coprocs,
        coprocs.contains(AccessController.class.getName()));
    assertTrue(
        "Observed coprocessors were: " + coprocs,
        coprocs.contains(MasterSpaceQuotaObserver.class.getName()));
  }
}
