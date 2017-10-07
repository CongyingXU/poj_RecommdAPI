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

module Shell
  module Commands
    class ListLocks < Command
      def help
        <<-EOF
List all locks in hbase. Examples:

  hbase> list_locks
EOF
      end

      def command
        list = admin.list_locks

        list.each do |lock|
          formatter.output_strln("#{lock.resourceType}(#{lock.resourceName})")

          case lock.lockType
          when org.apache.hadoop.hbase.procedure2.LockInfo::LockType::EXCLUSIVE then
            formatter.output_strln("Lock type: EXCLUSIVE, procedure: #{lock.exclusiveLockOwnerProcedure.procId}")
          when org.apache.hadoop.hbase.procedure2.LockInfo::LockType::SHARED then
            formatter.output_strln("Lock type: SHARED, count: #{lock.sharedLockCount}")
          end

          if lock.waitingProcedures.any?
            formatter.output_strln('Waiting procedures:')
            formatter.header(['Lock type', 'Procedure Id'])

            lock.waitingProcedures.each do |waitingProcedure|
              formatter.row([waitingProcedure.lockType.to_s, waitingProcedure.procedure.procId.to_s])
            end

            formatter.footer(lock.waitingProcedures.size)
          end

          formatter.output_strln('')
        end
      end
    end
  end
end
