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
package org.apache.hadoop.hbase.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.hadoop.hbase.classification.InterfaceAudience;

/**
 * Defines the way the ByteBuffers are created
 */
@InterfaceAudience.Private
public interface ByteBufferAllocator {

  /**
   * Allocates a bytebuffer
   * @param size the size of the bytebuffer
   * @param directByteBuffer indicator to create a direct bytebuffer
   * @return the bytebuffer that is created
   * @throws IOException exception thrown if there is an error while creating the ByteBuffer
   */
  ByteBuffer allocate(long size, boolean directByteBuffer) throws IOException;
}