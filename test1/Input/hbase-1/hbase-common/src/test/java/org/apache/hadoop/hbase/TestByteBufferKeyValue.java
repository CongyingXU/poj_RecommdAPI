/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.hadoop.hbase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.KeyValue.Type;
import org.apache.hadoop.hbase.testclassification.MiscTests;
import org.apache.hadoop.hbase.testclassification.SmallTests;
import org.apache.hadoop.hbase.util.ByteBufferUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({ MiscTests.class, SmallTests.class })
public class TestByteBufferKeyValue {
  private static final String QUAL2 = "qual2";
  private static final String FAM2 = "fam2";
  private static final String QUAL1 = "qual1";
  private static final String FAM1 = "fam1";
  private static final String ROW1 = "row1";
  private static final byte[] row1 = Bytes.toBytes(ROW1);
  private static final byte[] fam1 = Bytes.toBytes(FAM1);
  private static final byte[] fam2 = Bytes.toBytes(FAM2);
  private static final byte[] qual1 = Bytes.toBytes(QUAL1);
  private static final byte[] qual2 = Bytes.toBytes(QUAL2);
  private static final Tag t1 = new ArrayBackedTag((byte) 1, Bytes.toBytes("TAG1"));
  private static final Tag t2 = new ArrayBackedTag((byte) 2, Bytes.toBytes("TAG2"));
  private static final ArrayList<Tag> tags = new ArrayList<Tag>();
  static {
    tags.add(t1);
    tags.add(t2);
  }

  @Test
  public void testByteBufferBackedKeyValue() throws Exception {
    KeyValue kvCell = new KeyValue(row1, fam1, qual1, 0l, Type.Put, row1);
    ByteBuffer buf = ByteBuffer.allocateDirect(kvCell.getBuffer().length);
    ByteBufferUtils.copyFromArrayToBuffer(buf, kvCell.getBuffer(), 0, kvCell.getBuffer().length);
    ByteBufferCell offheapKV = new ByteBufferKeyValue(buf, 0, buf.capacity(), 0l);
    assertEquals(
      ROW1,
      ByteBufferUtils.toStringBinary(offheapKV.getRowByteBuffer(),
        offheapKV.getRowPosition(), offheapKV.getRowLength()));
    assertEquals(
      FAM1,
      ByteBufferUtils.toStringBinary(offheapKV.getFamilyByteBuffer(),
        offheapKV.getFamilyPosition(), offheapKV.getFamilyLength()));
    assertEquals(
      QUAL1,
      ByteBufferUtils.toStringBinary(offheapKV.getQualifierByteBuffer(),
        offheapKV.getQualifierPosition(), offheapKV.getQualifierLength()));
    assertEquals(
      ROW1,
      ByteBufferUtils.toStringBinary(offheapKV.getValueByteBuffer(),
        offheapKV.getValuePosition(), offheapKV.getValueLength()));
    assertEquals(0L, offheapKV.getTimestamp());
    assertEquals(Type.Put.getCode(), offheapKV.getTypeByte());

    // Use the array() APIs
    assertEquals(
      ROW1,
      Bytes.toStringBinary(offheapKV.getRowArray(),
        offheapKV.getRowOffset(), offheapKV.getRowLength()));
    assertEquals(
      FAM1,
      Bytes.toStringBinary(offheapKV.getFamilyArray(),
        offheapKV.getFamilyOffset(), offheapKV.getFamilyLength()));
    assertEquals(
      QUAL1,
      Bytes.toStringBinary(offheapKV.getQualifierArray(),
        offheapKV.getQualifierOffset(), offheapKV.getQualifierLength()));
    assertEquals(
      ROW1,
      Bytes.toStringBinary(offheapKV.getValueArray(),
        offheapKV.getValueOffset(), offheapKV.getValueLength()));
    assertEquals(0L, offheapKV.getTimestamp());
    assertEquals(Type.Put.getCode(), offheapKV.getTypeByte());

    kvCell = new KeyValue(row1, fam2, qual2, 0l, Type.Put, row1);
    buf = ByteBuffer.allocateDirect(kvCell.getBuffer().length);
    ByteBufferUtils.copyFromArrayToBuffer(buf, kvCell.getBuffer(), 0, kvCell.getBuffer().length);
    offheapKV = new ByteBufferKeyValue(buf, 0, buf.capacity(), 0l);
    assertEquals(
      FAM2,
      ByteBufferUtils.toStringBinary(offheapKV.getFamilyByteBuffer(),
        offheapKV.getFamilyPosition(), offheapKV.getFamilyLength()));
    assertEquals(
      QUAL2,
      ByteBufferUtils.toStringBinary(offheapKV.getQualifierByteBuffer(),
        offheapKV.getQualifierPosition(), offheapKV.getQualifierLength()));
    byte[] nullQualifier = new byte[0];
    kvCell = new KeyValue(row1, fam1, nullQualifier, 0L, Type.Put, row1);
    buf = ByteBuffer.allocateDirect(kvCell.getBuffer().length);
    ByteBufferUtils.copyFromArrayToBuffer(buf, kvCell.getBuffer(), 0, kvCell.getBuffer().length);
    offheapKV = new ByteBufferKeyValue(buf, 0, buf.capacity(), 0l);
    assertEquals(
      ROW1,
      ByteBufferUtils.toStringBinary(offheapKV.getRowByteBuffer(),
        offheapKV.getRowPosition(), offheapKV.getRowLength()));
    assertEquals(
      FAM1,
      ByteBufferUtils.toStringBinary(offheapKV.getFamilyByteBuffer(),
        offheapKV.getFamilyPosition(), offheapKV.getFamilyLength()));
    assertEquals(
      "",
      ByteBufferUtils.toStringBinary(offheapKV.getQualifierByteBuffer(),
        offheapKV.getQualifierPosition(), offheapKV.getQualifierLength()));
    assertEquals(
      ROW1,
      ByteBufferUtils.toStringBinary(offheapKV.getValueByteBuffer(),
        offheapKV.getValuePosition(), offheapKV.getValueLength()));
    assertEquals(0L, offheapKV.getTimestamp());
    assertEquals(Type.Put.getCode(), offheapKV.getTypeByte());
  }

  @Test
  public void testByteBufferBackedKeyValueWithTags() throws Exception {
    KeyValue kvCell = new KeyValue(row1, fam1, qual1, 0l, Type.Put, row1, tags);
    ByteBuffer buf = ByteBuffer.allocateDirect(kvCell.getBuffer().length);
    ByteBufferUtils.copyFromArrayToBuffer(buf, kvCell.getBuffer(), 0, kvCell.getBuffer().length);
    ByteBufferCell offheapKV = new ByteBufferKeyValue(buf, 0, buf.capacity(), 0l);
    assertEquals(
      ROW1,
      ByteBufferUtils.toStringBinary(offheapKV.getRowByteBuffer(),
        offheapKV.getRowPosition(), offheapKV.getRowLength()));
    assertEquals(
      FAM1,
      ByteBufferUtils.toStringBinary(offheapKV.getFamilyByteBuffer(),
        offheapKV.getFamilyPosition(), offheapKV.getFamilyLength()));
    assertEquals(
      QUAL1,
      ByteBufferUtils.toStringBinary(offheapKV.getQualifierByteBuffer(),
        offheapKV.getQualifierPosition(), offheapKV.getQualifierLength()));
    assertEquals(
      ROW1,
      ByteBufferUtils.toStringBinary(offheapKV.getValueByteBuffer(),
        offheapKV.getValuePosition(), offheapKV.getValueLength()));
    assertEquals(0L, offheapKV.getTimestamp());
    assertEquals(Type.Put.getCode(), offheapKV.getTypeByte());
    // change tags to handle both onheap and offheap stuff
    List<Tag> resTags = TagUtil.asList(offheapKV.getTagsArray(), offheapKV.getTagsOffset(),
        offheapKV.getTagsLength());
    Tag tag1 = resTags.get(0);
    assertEquals(t1.getType(), tag1.getType());
    assertEquals(TagUtil.getValueAsString(t1), TagUtil.getValueAsString(tag1));
    Tag tag2 = resTags.get(1);
    assertEquals(tag2.getType(), tag2.getType());
    assertEquals(TagUtil.getValueAsString(t2), TagUtil.getValueAsString(tag2));
    Tag res = CellUtil.getTag(offheapKV, (byte) 2);
    assertEquals(TagUtil.getValueAsString(t2), TagUtil.getValueAsString(tag2));
    res = CellUtil.getTag(offheapKV, (byte) 3);
    assertNull(res);
  }

  @Test
  public void testGetKeyMethods() throws Exception {
    KeyValue kvCell = new KeyValue(row1, fam1, qual1, 0l, Type.Put, row1, tags);
    ByteBuffer buf = ByteBuffer.allocateDirect(kvCell.getKeyLength());
    ByteBufferUtils.copyFromArrayToBuffer(buf, kvCell.getBuffer(), kvCell.getKeyOffset(),
      kvCell.getKeyLength());
    ByteBufferCell offheapKeyOnlyKV = new ByteBufferKeyOnlyKeyValue(buf, 0, buf.capacity());
    assertEquals(
      ROW1,
      ByteBufferUtils.toStringBinary(offheapKeyOnlyKV.getRowByteBuffer(),
        offheapKeyOnlyKV.getRowPosition(), offheapKeyOnlyKV.getRowLength()));
    assertEquals(
      FAM1,
      ByteBufferUtils.toStringBinary(offheapKeyOnlyKV.getFamilyByteBuffer(),
        offheapKeyOnlyKV.getFamilyPosition(), offheapKeyOnlyKV.getFamilyLength()));
    assertEquals(
      QUAL1,
      ByteBufferUtils.toStringBinary(offheapKeyOnlyKV.getQualifierByteBuffer(),
        offheapKeyOnlyKV.getQualifierPosition(),
        offheapKeyOnlyKV.getQualifierLength()));
    assertEquals(0L, offheapKeyOnlyKV.getTimestamp());
    assertEquals(Type.Put.getCode(), offheapKeyOnlyKV.getTypeByte());
  }
}
