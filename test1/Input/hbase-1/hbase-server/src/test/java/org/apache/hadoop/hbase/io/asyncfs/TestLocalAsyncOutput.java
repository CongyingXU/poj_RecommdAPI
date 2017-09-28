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
package org.apache.hadoop.hbase.io.asyncfs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseCommonTestingUtility;
import org.apache.hadoop.hbase.testclassification.MiscTests;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({ MiscTests.class, SmallTests.class })
public class TestLocalAsyncOutput {

  private static EventLoopGroup GROUP = new NioEventLoopGroup();

  private static Class<? extends Channel> CHANNEL_CLASS = NioSocketChannel.class;

  private static final HBaseCommonTestingUtility TEST_UTIL = new HBaseCommonTestingUtility();

  @AfterClass
  public static void tearDownAfterClass() throws IOException {
    TEST_UTIL.cleanupTestDir();
    GROUP.shutdownGracefully();
  }

  @Test
  public void test() throws IOException, InterruptedException, ExecutionException {
    Path f = new Path(TEST_UTIL.getDataTestDir(), "test");
    FileSystem fs = FileSystem.getLocal(TEST_UTIL.getConfiguration());
    AsyncFSOutput out = AsyncFSOutputHelper.createOutput(fs, f, false, true,
      fs.getDefaultReplication(f), fs.getDefaultBlockSize(f), GROUP.next(), CHANNEL_CLASS);
    byte[] b = new byte[10];
    ThreadLocalRandom.current().nextBytes(b);
    out.write(b);
    assertEquals(b.length, out.flush(true).get().longValue());
    out.close();
    assertEquals(b.length, fs.getFileStatus(f).getLen());
    byte[] actual = new byte[b.length];
    try (FSDataInputStream in = fs.open(f)) {
      in.readFully(actual);
    }
    assertArrayEquals(b, actual);
  }
}
