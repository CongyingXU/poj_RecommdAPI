// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/protobuf/source_context.proto

package org.apache.hadoop.hbase.shaded.com.google.protobuf;

public final class SourceContextProto {
  private SourceContextProto() {}
  public static void registerAllExtensions(
      org.apache.hadoop.hbase.shaded.com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      org.apache.hadoop.hbase.shaded.com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (org.apache.hadoop.hbase.shaded.com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final org.apache.hadoop.hbase.shaded.com.google.protobuf.Descriptors.Descriptor
    internal_static_google_protobuf_SourceContext_descriptor;
  static final 
    org.apache.hadoop.hbase.shaded.com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_google_protobuf_SourceContext_fieldAccessorTable;

  public static org.apache.hadoop.hbase.shaded.com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  org.apache.hadoop.hbase.shaded.com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n$google/protobuf/source_context.proto\022\017" +
      "google.protobuf\"\"\n\rSourceContext\022\021\n\tfile" +
      "_name\030\001 \001(\tB\225\001\n\023com.google.protobufB\022Sou" +
      "rceContextProtoP\001ZAgoogle.golang.org/gen" +
      "proto/protobuf/source_context;source_con" +
      "text\242\002\003GPB\252\002\036Google.Protobuf.WellKnownTy" +
      "pesb\006proto3"
    };
    org.apache.hadoop.hbase.shaded.com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new org.apache.hadoop.hbase.shaded.com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public org.apache.hadoop.hbase.shaded.com.google.protobuf.ExtensionRegistry assignDescriptors(
              org.apache.hadoop.hbase.shaded.com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    org.apache.hadoop.hbase.shaded.com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new org.apache.hadoop.hbase.shaded.com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_google_protobuf_SourceContext_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_google_protobuf_SourceContext_fieldAccessorTable = new
      org.apache.hadoop.hbase.shaded.com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_google_protobuf_SourceContext_descriptor,
        new java.lang.String[] { "FileName", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
