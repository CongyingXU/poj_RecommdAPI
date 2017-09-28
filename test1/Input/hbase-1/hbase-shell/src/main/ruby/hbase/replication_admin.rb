#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

include Java

java_import org.apache.hadoop.hbase.client.replication.ReplicationAdmin
java_import org.apache.hadoop.hbase.client.replication.ReplicationSerDeHelper
java_import org.apache.hadoop.hbase.replication.ReplicationPeerConfig
java_import org.apache.hadoop.hbase.util.Bytes
java_import org.apache.hadoop.hbase.zookeeper.ZKConfig
java_import org.apache.hadoop.hbase.TableName

# Wrapper for org.apache.hadoop.hbase.client.replication.ReplicationAdmin

module Hbase
  class RepAdmin
    include HBaseConstants

    def initialize(configuration)
      @replication_admin = ReplicationAdmin.new(configuration)
      @configuration = configuration
      @admin = ConnectionFactory.createConnection(configuration).getAdmin
    end

    #----------------------------------------------------------------------------------------------
    # Add a new peer cluster to replicate to
    def add_peer(id, args = {}, peer_tableCFs = nil)
      if args.is_a?(Hash)
        unless peer_tableCFs.nil?
          raise(ArgumentError, 'peer_tableCFs should be specified as TABLE_CFS in args')
        end

        endpoint_classname = args.fetch(ENDPOINT_CLASSNAME, nil)
        cluster_key = args.fetch(CLUSTER_KEY, nil)

        # Handle cases where custom replication endpoint and cluster key are either both provided
        # or neither are provided
        if endpoint_classname.nil? && cluster_key.nil?
          raise(ArgumentError, 'Either ENDPOINT_CLASSNAME or CLUSTER_KEY must be specified.')
        end

        # Cluster Key is required for ReplicationPeerConfig for a custom replication endpoint
        if !endpoint_classname.nil? && cluster_key.nil?
          cluster_key = ZKConfig.getZooKeeperClusterKey(@configuration)
        end

        # Optional parameters
        config = args.fetch(CONFIG, nil)
        data = args.fetch(DATA, nil)
        table_cfs = args.fetch(TABLE_CFS, nil)
        namespaces = args.fetch(NAMESPACES, nil)

        # Create and populate a ReplicationPeerConfig
        replication_peer_config = ReplicationPeerConfig.new
        replication_peer_config.set_cluster_key(cluster_key)

        unless endpoint_classname.nil?
          replication_peer_config.set_replication_endpoint_impl(endpoint_classname)
        end

        unless config.nil?
          replication_peer_config.get_configuration.put_all(config)
        end

        unless data.nil?
          # Convert Strings to Bytes for peer_data
          peer_data = replication_peer_config.get_peer_data
          data.each do |key, val|
            peer_data.put(Bytes.to_bytes(key), Bytes.to_bytes(val))
          end
        end

        unless namespaces.nil?
          ns_set = java.util.HashSet.new
          namespaces.each do |n|
            ns_set.add(n)
          end
          replication_peer_config.set_namespaces(ns_set)
        end

        unless table_cfs.nil?
          # convert table_cfs to TableName
          map = java.util.HashMap.new
          table_cfs.each do |key, val|
            map.put(org.apache.hadoop.hbase.TableName.valueOf(key), val)
          end
          replication_peer_config.set_table_cfs_map(map)
        end
        @admin.addReplicationPeer(id, replication_peer_config)
      else
        raise(ArgumentError, 'args must be a Hash')
      end
    end

    #----------------------------------------------------------------------------------------------
    # Remove a peer cluster, stops the replication
    def remove_peer(id)
      @admin.removeReplicationPeer(id)
    end

    #---------------------------------------------------------------------------------------------
    # Show replcated tables/column families, and their ReplicationType
    def list_replicated_tables(regex = '.*')
      pattern = java.util.regex.Pattern.compile(regex)
      list = @admin.listReplicatedTableCFs
      list.select { |t| pattern.match(t.getTable.getNameAsString) }
    end

    #----------------------------------------------------------------------------------------------
    # List all peer clusters
    def list_peers
      @admin.listReplicationPeers
    end

    #----------------------------------------------------------------------------------------------
    # Restart the replication stream to the specified peer
    def enable_peer(id)
      @admin.enableReplicationPeer(id)
    end

    #----------------------------------------------------------------------------------------------
    # Stop the replication stream to the specified peer
    def disable_peer(id)
      @admin.disableReplicationPeer(id)
    end

    #----------------------------------------------------------------------------------------------
    # Show the current tableCFs config for the specified peer
    def show_peer_tableCFs(id)
      rpc = @admin.getReplicationPeerConfig(id)
      ReplicationSerDeHelper.convertToString(rpc.getTableCFsMap)
    end

    #----------------------------------------------------------------------------------------------
    # Set new tableCFs config for the specified peer
    def set_peer_tableCFs(id, tableCFs)
      unless tableCFs.nil?
        # convert tableCFs to TableName
        map = java.util.HashMap.new
        tableCFs.each do |key, val|
          map.put(org.apache.hadoop.hbase.TableName.valueOf(key), val)
        end
        rpc = get_peer_config(id)
        unless rpc.nil?
          rpc.setTableCFsMap(map)
          @admin.updateReplicationPeerConfig(id, rpc)
        end
      end
    end

    #----------------------------------------------------------------------------------------------
    # Append a tableCFs config for the specified peer
    def append_peer_tableCFs(id, tableCFs)
      unless tableCFs.nil?
        # convert tableCFs to TableName
        map = java.util.HashMap.new
        tableCFs.each do |key, val|
          map.put(org.apache.hadoop.hbase.TableName.valueOf(key), val)
        end
      end
      @admin.appendReplicationPeerTableCFs(id, map)
    end

    #----------------------------------------------------------------------------------------------
    # Remove some tableCFs from the tableCFs config of the specified peer
    def remove_peer_tableCFs(id, tableCFs)
      unless tableCFs.nil?
        # convert tableCFs to TableName
        map = java.util.HashMap.new
        tableCFs.each do |key, val|
          map.put(org.apache.hadoop.hbase.TableName.valueOf(key), val)
        end
      end
      @admin.removeReplicationPeerTableCFs(id, map)
    end

    # Set new namespaces config for the specified peer
    def set_peer_namespaces(id, namespaces)
      unless namespaces.nil?
        ns_set = java.util.HashSet.new
        namespaces.each do |n|
          ns_set.add(n)
        end
        rpc = get_peer_config(id)
        unless rpc.nil?
          rpc.setNamespaces(ns_set)
          @admin.updateReplicationPeerConfig(id, rpc)
        end
      end
    end

    # Add some namespaces for the specified peer
    def add_peer_namespaces(id, namespaces)
      unless namespaces.nil?
        rpc = get_peer_config(id)
        unless rpc.nil?
          ns_set = rpc.getNamespaces
          ns_set = java.util.HashSet.new if ns_set.nil?
          namespaces.each do |n|
            ns_set.add(n)
          end
          rpc.setNamespaces(ns_set)
          @admin.updateReplicationPeerConfig(id, rpc)
        end
      end
    end

    # Remove some namespaces for the specified peer
    def remove_peer_namespaces(id, namespaces)
      unless namespaces.nil?
        rpc = get_peer_config(id)
        unless rpc.nil?
          ns_set = rpc.getNamespaces
          unless ns_set.nil?
            namespaces.each do |n|
              ns_set.remove(n)
            end
          end
          rpc.setNamespaces(ns_set)
          @admin.updateReplicationPeerConfig(id, rpc)
        end
      end
    end

    # Show the current namespaces config for the specified peer
    def show_peer_namespaces(peer_config)
      namespaces = peer_config.get_namespaces
      if !namespaces.nil?
        namespaces = java.util.ArrayList.new(namespaces)
        java.util.Collections.sort(namespaces)
        return namespaces.join(';')
      else
        return nil
      end
    end

    # Set new bandwidth config for the specified peer
    def set_peer_bandwidth(id, bandwidth)
      rpc = get_peer_config(id)
      unless rpc.nil?
        rpc.setBandwidth(bandwidth)
        @admin.updateReplicationPeerConfig(id, rpc)
      end
    end

    #----------------------------------------------------------------------------------------------
    # Enables a table's replication switch
    def enable_tablerep(table_name)
      tableName = TableName.valueOf(table_name)
      @admin.enableTableReplication(tableName)
    end

    #----------------------------------------------------------------------------------------------
    # Disables a table's replication switch
    def disable_tablerep(table_name)
      tableName = TableName.valueOf(table_name)
      @admin.disableTableReplication(tableName)
    end

    def list_peer_configs
      map = java.util.HashMap.new
      peers = @admin.listReplicationPeers
      peers.each do |peer|
        map.put(peer.getPeerId, peer.getPeerConfig)
      end
      map
    end

    def get_peer_config(id)
      @admin.getReplicationPeerConfig(id)
    end

    def update_peer_config(id, args = {})
      # Optional parameters
      config = args.fetch(CONFIG, nil)
      data = args.fetch(DATA, nil)

      # Create and populate a ReplicationPeerConfig
      replication_peer_config = get_peer_config(id)
      unless config.nil?
        replication_peer_config.get_configuration.put_all(config)
      end

      unless data.nil?
        # Convert Strings to Bytes for peer_data
        peer_data = replication_peer_config.get_peer_data
        data.each do |key, val|
          peer_data.put(Bytes.to_bytes(key), Bytes.to_bytes(val))
        end
      end

      @admin.updateReplicationPeerConfig(id, replication_peer_config)
    end
  end
end
