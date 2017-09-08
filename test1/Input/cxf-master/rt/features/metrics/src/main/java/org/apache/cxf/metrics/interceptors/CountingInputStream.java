/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.metrics.interceptors;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class CountingInputStream extends FilterInputStream {

    private long count;
    private long mark = -1;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    public long getCount() {
        return count;
    }

    public int read() throws IOException {
        int result = in.read();
        if (result != -1) {
            count++;
        }
        return result;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int result = in.read(b, off, len);
        if (result != -1) {
            count += result;
        }
        return result;
    }

    public long skip(long n) throws IOException {
        long result = in.skip(n);
        count += result;
        return result;
    }

    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
        mark = count;
    }

    public synchronized void reset() throws IOException {
        if (!in.markSupported()) {
            throw new IOException("Mark not supported");
        }
        if (mark == -1) {
            throw new IOException("Mark not set");
        }

        in.reset();
        count = mark;
    }
}