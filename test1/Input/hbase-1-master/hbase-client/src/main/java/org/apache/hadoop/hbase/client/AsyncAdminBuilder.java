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

import static org.apache.hadoop.hbase.client.ConnectionUtils.retries2Attempts;

import java.util.concurrent.TimeUnit;

import org.apache.hadoop.hbase.classification.InterfaceAudience;

/**
 * For creating {@link AsyncAdmin}. The implementation should have default configurations set before
 * returning the builder to user. So users are free to only set the configs they care about to
 * create a new AsyncAdmin instance.
 */
@InterfaceAudience.Public
public interface AsyncAdminBuilder<T extends AsyncAdmin> {

  /**
   * Set timeout for a whole admin operation. Operation timeout and max attempt times(or max retry
   * times) are both limitations for retrying, we will stop retrying when we reach any of the
   * limitations.
   * @param timeout
   * @param unit
   * @return this for invocation chaining
   */
  AsyncAdminBuilder<T> setOperationTimeout(long timeout, TimeUnit unit);

  /**
   * Set timeout for each rpc request.
   * @param timeout
   * @param unit
   * @return this for invocation chaining
   */
  AsyncAdminBuilder<T> setRpcTimeout(long timeout, TimeUnit unit);

  /**
   * Set the base pause time for retrying. We use an exponential policy to generate sleep time when
   * retrying.
   * @param timeout
   * @param unit
   * @return this for invocation chaining
   */
  AsyncAdminBuilder<T> setRetryPause(long timeout, TimeUnit unit);

  /**
   * Set the max retry times for an admin operation. Usually it is the max attempt times minus 1.
   * Operation timeout and max attempt times(or max retry times) are both limitations for retrying,
   * we will stop retrying when we reach any of the limitations.
   * @param maxRetries
   * @return this for invocation chaining
   */
  default AsyncAdminBuilder<T> setMaxRetries(int maxRetries) {
    return setMaxAttempts(retries2Attempts(maxRetries));
  }

  /**
   * Set the max attempt times for an admin operation. Usually it is the max retry times plus 1.
   * Operation timeout and max attempt times(or max retry times) are both limitations for retrying,
   * we will stop retrying when we reach any of the limitations.
   * @param maxAttempts
   * @return this for invocation chaining
   */
  AsyncAdminBuilder<T> setMaxAttempts(int maxAttempts);

  /**
   * Set the number of retries that are allowed before we start to log.
   * @param startLogErrorsCnt
   * @return this for invocation chaining
   */
  AsyncAdminBuilder<T> setStartLogErrorsCnt(int startLogErrorsCnt);

  /**
   * Create a {@link AsyncAdmin} instance.
   * @return a {@link AsyncAdmin} instance
   */
  T build();
}