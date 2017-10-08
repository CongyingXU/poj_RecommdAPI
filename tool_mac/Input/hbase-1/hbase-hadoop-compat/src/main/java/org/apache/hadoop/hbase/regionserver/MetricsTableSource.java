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

package org.apache.hadoop.hbase.regionserver;

/**
 * This interface will be implemented to allow region server to push table metrics into
 * MetricsRegionAggregateSource that will in turn push data to the Hadoop metrics system.
 */
public interface MetricsTableSource extends Comparable<MetricsTableSource> {

  String READ_REQUEST_COUNT = "readRequestCount";
  String READ_REQUEST_COUNT_DESC = "Number fo read requests";
  String WRITE_REQUEST_COUNT = "writeRequestCount";
  String WRITE_REQUEST_COUNT_DESC = "Number fo write requests";
  String TOTAL_REQUEST_COUNT = "totalRequestCount";
  String TOTAL_REQUEST_COUNT_DESC = "Number fo total requests";
  String MEMSTORE_SIZE = "memstoreSize";
  String MEMSTORE_SIZE_DESC = "The size of memory stores";
  String STORE_FILE_SIZE = "storeFileSize";
  String STORE_FILE_SIZE_DESC = "The size of store files size";
  String TABLE_SIZE = "tableSize";
  String TABLE_SIZE_DESC = "Total size of the table in the region server";

  String getTableName();

  /**
   * Close the table's metrics as all the region are closing.
   */
  void close();

  /**
   * Get the aggregate source to which this reports.
   */
  MetricsTableAggregateSource getAggregateSource();

}
