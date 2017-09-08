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
package org.apache.hadoop.hbase;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.hadoop.hbase.classification.InterfaceAudience;
import org.apache.hadoop.hbase.util.ByteBufferUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.ClassSize;

import com.google.common.annotations.VisibleForTesting;

/**
 * This Cell is an implementation of {@link ByteBufferCell} where the data resides in
 * off heap/ on heap ByteBuffer
 */
@InterfaceAudience.Private
public class ByteBufferKeyValue extends ByteBufferCell implements ExtendedCell {

  protected final ByteBuffer buf;
  protected final int offset;
  protected final int length;
  private long seqId = 0;

  public static final int FIXED_OVERHEAD = ClassSize.OBJECT + ClassSize.REFERENCE
      + (2 * Bytes.SIZEOF_INT) + Bytes.SIZEOF_LONG;

  public ByteBufferKeyValue(ByteBuffer buf, int offset, int length, long seqId) {
    this.buf = buf;
    this.offset = offset;
    this.length = length;
    this.seqId = seqId;
  }

  public ByteBufferKeyValue(ByteBuffer buf, int offset, int length) {
    this.buf = buf;
    this.offset = offset;
    this.length = length;
  }

  @VisibleForTesting
  public ByteBuffer getBuffer() {
    return this.buf;
  }

  @VisibleForTesting
  public int getOffset() {
    return this.offset;
  }

  @Override
  public byte[] getRowArray() {
    return CellUtil.cloneRow(this);
  }

  @Override
  public int getRowOffset() {
    return 0;
  }

  @Override
  public short getRowLength() {
    return getRowLen();
  }

  private short getRowLen() {
    return ByteBufferUtils.toShort(this.buf, this.offset + KeyValue.ROW_OFFSET);
  }

  @Override
  public byte[] getFamilyArray() {
    return CellUtil.cloneFamily(this);
  }

  @Override
  public int getFamilyOffset() {
    return 0;
  }

  @Override
  public byte getFamilyLength() {
    return getFamilyLength(getFamilyLengthPosition());
  }

  private int getFamilyLengthPosition() {
    return this.offset + KeyValue.ROW_KEY_OFFSET
        + getRowLen();
  }

  private byte getFamilyLength(int famLenPos) {
    return ByteBufferUtils.toByte(this.buf, famLenPos);
  }

  @Override
  public byte[] getQualifierArray() {
    return CellUtil.cloneQualifier(this);
  }

  @Override
  public int getQualifierOffset() {
    return 0;
  }

  @Override
  public int getQualifierLength() {
    return getQualifierLength(getRowLength(), getFamilyLength());
  }

  private int getQualifierLength(int rlength, int flength) {
    return getKeyLen()
        - (int) KeyValue.getKeyDataStructureSize(rlength, flength, 0);
  }

  @Override
  public long getTimestamp() {
    int offset = getTimestampOffset(getKeyLen());
    return ByteBufferUtils.toLong(this.buf, offset);
  }

  private int getKeyLen() {
    return ByteBufferUtils.toInt(this.buf, this.offset);
  }

  private int getTimestampOffset(int keyLen) {
    return this.offset + KeyValue.ROW_OFFSET + keyLen - KeyValue.TIMESTAMP_TYPE_SIZE;
  }

  @Override
  public byte getTypeByte() {
    return ByteBufferUtils.toByte(this.buf,
      this.offset + getKeyLen() - 1 + KeyValue.ROW_OFFSET);
  }

  @Override
  public long getSequenceId() {
    return this.seqId;
  }

  public void setSequenceId(long seqId) {
    this.seqId = seqId;
  }

  @Override
  public byte[] getValueArray() {
    return CellUtil.cloneValue(this);
  }

  @Override
  public int getValueOffset() {
    return 0;
  }

  @Override
  public int getValueLength() {
    return ByteBufferUtils.toInt(this.buf, this.offset + Bytes.SIZEOF_INT);
  }

  @Override
  public byte[] getTagsArray() {
    return CellUtil.cloneTags(this);
  }

  @Override
  public int getTagsOffset() {
    return 0;
  }

  @Override
  public int getTagsLength() {
    int tagsLen = this.length - (getKeyLen() + getValueLength()
        + KeyValue.KEYVALUE_INFRASTRUCTURE_SIZE);
    if (tagsLen > 0) {
      // There are some Tag bytes in the byte[]. So reduce 2 bytes which is
      // added to denote the tags
      // length
      tagsLen -= KeyValue.TAGS_LENGTH_SIZE;
    }
    return tagsLen;
  }

  @Override
  public ByteBuffer getRowByteBuffer() {
    return this.buf;
  }

  @Override
  public int getRowPosition() {
    return this.offset + KeyValue.ROW_KEY_OFFSET;
  }

  @Override
  public ByteBuffer getFamilyByteBuffer() {
    return this.buf;
  }

  @Override
  public int getFamilyPosition() {
    return getFamilyLengthPosition() + Bytes.SIZEOF_BYTE;
  }

  @Override
  public ByteBuffer getQualifierByteBuffer() {
    return this.buf;
  }

  @Override
  public int getQualifierPosition() {
    return getFamilyPosition() + getFamilyLength();
  }

  @Override
  public ByteBuffer getValueByteBuffer() {
    return this.buf;
  }

  @Override
  public int getValuePosition() {
    return this.offset + KeyValue.ROW_OFFSET + getKeyLen();
  }

  @Override
  public ByteBuffer getTagsByteBuffer() {
    return this.buf;
  }

  @Override
  public int getTagsPosition() {
    int tagsLen = getTagsLength();
    if (tagsLen == 0) {
      return this.offset + this.length;
    }
    return this.offset + this.length - tagsLen;
  }

  @Override
  public long heapSize() {
    if (this.buf.hasArray()) {
      return ClassSize.align(FIXED_OVERHEAD + length);
    }
    return ClassSize.align(FIXED_OVERHEAD);
  }

  @Override
  public int write(OutputStream out, boolean withTags) throws IOException {
    int length = getSerializedSize(withTags);
    ByteBufferUtils.copyBufferToStream(out, this.buf, this.offset, length);
    return length;
  }

  @Override
  public int getSerializedSize(boolean withTags) {
    if (withTags) {
      return this.length;
    }
    return getKeyLen() + this.getValueLength()
        + KeyValue.KEYVALUE_INFRASTRUCTURE_SIZE;
  }

  @Override
  public void write(ByteBuffer buf, int offset) {
    ByteBufferUtils.copyFromBufferToBuffer(this.buf, buf, this.offset, offset, this.length);
  }

  @Override
  public String toString() {
    return CellUtil.toString(this, true);
  }

  @Override
  public void setTimestamp(long ts) throws IOException {
    ByteBufferUtils.copyFromArrayToBuffer(this.buf, this.getTimestampOffset(), Bytes.toBytes(ts), 0,
      Bytes.SIZEOF_LONG);
  }

  private int getTimestampOffset() {
    return this.offset + KeyValue.KEYVALUE_INFRASTRUCTURE_SIZE
        + getKeyLen() - KeyValue.TIMESTAMP_TYPE_SIZE;
  }

  @Override
  public void setTimestamp(byte[] ts, int tsOffset) throws IOException {
    ByteBufferUtils.copyFromArrayToBuffer(this.buf, this.getTimestampOffset(), ts, tsOffset,
        Bytes.SIZEOF_LONG);
  }

  @Override
  public Cell deepClone() {
    byte[] copy = new byte[this.length];
    ByteBufferUtils.copyFromBufferToArray(copy, this.buf, this.offset, 0, this.length);
    KeyValue kv = new KeyValue(copy, 0, copy.length);
    kv.setSequenceId(this.getSequenceId());
    return kv;
  }

  /**
   * Needed doing 'contains' on List. Only compares the key portion, not the value.
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Cell)) {
      return false;
    }
    return CellUtil.equals(this, (Cell) other);
  }

  /**
   * In line with {@link #equals(Object)}, only uses the key portion, not the value.
   */
  @Override
  public int hashCode() {
    return calculateHashForKey(this);
  }

  private int calculateHashForKey(ByteBufferCell cell) {
    int rowHash = ByteBufferUtils.hashCode(cell.getRowByteBuffer(), cell.getRowPosition(),
      cell.getRowLength());
    int familyHash = ByteBufferUtils.hashCode(cell.getFamilyByteBuffer(), cell.getFamilyPosition(),
      cell.getFamilyLength());
    int qualifierHash = ByteBufferUtils.hashCode(cell.getQualifierByteBuffer(),
      cell.getQualifierPosition(), cell.getQualifierLength());

    int hash = 31 * rowHash + familyHash;
    hash = 31 * hash + qualifierHash;
    hash = 31 * hash + (int) cell.getTimestamp();
    hash = 31 * hash + cell.getTypeByte();
    return hash;
  }
}
