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
package org.apache.cxf.systest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.htrace.core.HTraceConfiguration;
import org.apache.htrace.core.Span;
import org.apache.htrace.core.SpanReceiver;

/**
 * Test HTrace Span receiver
 */
public class TestSpanReceiver extends SpanReceiver {
    private static List<Span> spans = new ArrayList<>();

    public TestSpanReceiver(final HTraceConfiguration conf) {
    }

    public Collection<Span> getSpans() {
        return spans;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void receiveSpan(Span span) {
        spans.add(span);
    }

    public static void clear() {
        spans.clear();
    }

    public static List<Span> getAllSpans() {
        return spans;
    }

}