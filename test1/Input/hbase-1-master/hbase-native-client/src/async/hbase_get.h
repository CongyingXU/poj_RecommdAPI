/*
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
 *
 */

#ifndef ASYNC_HBASE_GET_H_
#define ASYNC_HBASE_GET_H_

#ifdef __cplusplus
extern "C" {
#endif

#include "core/hbase_macros.h"
#include "core/hbase_types.h"

/**
 * Allocate a new get structure.
 * Ownership passes to the caller.
 */
HBASE_API int32_t hb_get_create(hb_get_t * get_ptr);

/**
 * Destroy and free a get structure.
 */
HBASE_API int32_t hb_get_destroy(hb_get_t  get);

/**
 * set the row of this get.
 */
HBASE_API int32_t hb_get_set_row(hb_get_t get, hb_byte_t * row,
    size_t row_length);

/**
 * Set the table.
 */
HBASE_API int32_t hb_get_set_table(hb_get_t get,
    char * table, size_t table_length);

/**
 * Set the namespace this get is targeting.
 */
HBASE_API int32_t hb_get_set_namespace(hb_get_t get,
    char * name_space, size_t name_space_length);

/*
 * get call back typedef.
 */
typedef void (* hb_get_cb)(int32_t status, hb_client_t client,
    hb_get_t get, hb_result_t results, void * extra);

HBASE_API int32_t hb_get_send(hb_client_t client,
    hb_get_t get, hb_get_cb cb, void * extra);


#ifdef __cplusplus
}  // extern "C"
#endif  // __cplusplus

#endif  // ASYNC_HBASE_GET_H_
