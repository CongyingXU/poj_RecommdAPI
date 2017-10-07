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

package org.apache.hadoop.hbase.master.procedure;

import com.google.common.collect.ArrayListMultimap;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.ipc.RemoteException;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.DoNotRetryIOException;
import org.apache.hadoop.hbase.ipc.ServerNotRunningYetException;
import org.apache.hadoop.hbase.master.MasterServices;
import org.apache.hadoop.hbase.master.ServerListener;
import org.apache.hadoop.hbase.procedure2.RemoteProcedureDispatcher;
import org.apache.hadoop.hbase.shaded.com.google.protobuf.ServiceException;
import org.apache.hadoop.hbase.shaded.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.shaded.protobuf.RequestConverter;
import org.apache.hadoop.hbase.shaded.protobuf.generated.AdminProtos.AdminService;
import org.apache.hadoop.hbase.shaded.protobuf.generated.AdminProtos.CloseRegionRequest;
import org.apache.hadoop.hbase.shaded.protobuf.generated.AdminProtos.CloseRegionResponse;
import org.apache.hadoop.hbase.shaded.protobuf.generated.AdminProtos.ExecuteProceduresRequest;
import org.apache.hadoop.hbase.shaded.protobuf.generated.AdminProtos.ExecuteProceduresResponse;
import org.apache.hadoop.hbase.shaded.protobuf.generated.AdminProtos.OpenRegionRequest;
import org.apache.hadoop.hbase.shaded.protobuf.generated.AdminProtos.OpenRegionResponse;
import org.apache.hadoop.hbase.util.EnvironmentEdgeManager;

/**
 * A remote procecdure dispatcher for regionservers.
 */
public class RSProcedureDispatcher
    extends RemoteProcedureDispatcher<MasterProcedureEnv, ServerName>
    implements ServerListener {
  private static final Log LOG = LogFactory.getLog(RSProcedureDispatcher.class);

  public static final String RS_RPC_STARTUP_WAIT_TIME_CONF_KEY =
      "hbase.regionserver.rpc.startup.waittime";
  private static final int DEFAULT_RS_RPC_STARTUP_WAIT_TIME = 60000;

  private static final int RS_VERSION_WITH_EXEC_PROCS = 0x0200000; // 2.0

  protected final MasterServices master;
  protected final long rsStartupWaitTime;

  public RSProcedureDispatcher(final MasterServices master) {
    super(master.getConfiguration());

    this.master = master;
    this.rsStartupWaitTime = master.getConfiguration().getLong(
      RS_RPC_STARTUP_WAIT_TIME_CONF_KEY, DEFAULT_RS_RPC_STARTUP_WAIT_TIME);
  }

  @Override
  public boolean start() {
    if (!super.start()) {
      return false;
    }

    master.getServerManager().registerListener(this);
    for (ServerName serverName: master.getServerManager().getOnlineServersList()) {
      addNode(serverName);
    }
    return true;
  }

  @Override
  public boolean stop() {
    if (!super.stop()) {
      return false;
    }

    master.getServerManager().unregisterListener(this);
    return true;
  }

  @Override
  protected void remoteDispatch(final ServerName serverName,
      final Set<RemoteProcedure> operations) {
    final int rsVersion = master.getAssignmentManager().getServerVersion(serverName);
    if (rsVersion >= RS_VERSION_WITH_EXEC_PROCS) {
      LOG.info(String.format(
        "Using procedure batch rpc execution for serverName=%s version=%s",
        serverName, rsVersion));
      submitTask(new ExecuteProceduresRemoteCall(serverName, operations));
    } else {
      LOG.info(String.format(
        "Fallback to compat rpc execution for serverName=%s version=%s",
        serverName, rsVersion));
      submitTask(new CompatRemoteProcedureResolver(serverName, operations));
    }
  }

  protected void abortPendingOperations(final ServerName serverName,
      final Set<RemoteProcedure> operations) {
    // TODO: Replace with a ServerNotOnlineException()
    final IOException e = new DoNotRetryIOException("server not online " + serverName);
    final MasterProcedureEnv env = master.getMasterProcedureExecutor().getEnvironment();
    for (RemoteProcedure proc: operations) {
      proc.remoteCallFailed(env, serverName, e);
    }
  }

  public void serverAdded(final ServerName serverName) {
    addNode(serverName);
  }

  public void serverRemoved(final ServerName serverName) {
    removeNode(serverName);
  }

  /**
   * Base remote call
   */
  protected abstract class AbstractRSRemoteCall implements Callable<Void> {
    private final ServerName serverName;

    private int numberOfAttemptsSoFar = 0;
    private long maxWaitTime = -1;

    public AbstractRSRemoteCall(final ServerName serverName) {
      this.serverName = serverName;
    }

    public abstract Void call();

    protected AdminService.BlockingInterface getRsAdmin() throws IOException {
      final AdminService.BlockingInterface admin = master.getServerManager().getRsAdmin(serverName);
      if (admin == null) {
        throw new IOException("Attempting to send OPEN RPC to server " + getServerName() +
          " failed because no RPC connection found to this server");
      }
      return admin;
    }

    protected ServerName getServerName() {
      return serverName;
    }

    protected boolean scheduleForRetry(final IOException e) {
      // Should we wait a little before retrying? If the server is starting it's yes.
      final boolean hold = (e instanceof ServerNotRunningYetException);
      if (hold) {
        LOG.warn(String.format("waiting a little before trying on the same server=%s try=%d",
            serverName, numberOfAttemptsSoFar), e);
        long now = EnvironmentEdgeManager.currentTime();
        if (now < getMaxWaitTime()) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("server is not yet up; waiting up to %dms",
              (getMaxWaitTime() - now)), e);
          }
          submitTask(this, 100, TimeUnit.MILLISECONDS);
          return true;
        }

        LOG.warn(String.format("server %s is not up for a while; try a new one", serverName), e);
        return false;
      }

      // In case socket is timed out and the region server is still online,
      // the openRegion RPC could have been accepted by the server and
      // just the response didn't go through.  So we will retry to
      // open the region on the same server.
      final boolean retry = !hold && (e instanceof SocketTimeoutException
          && master.getServerManager().isServerOnline(serverName));
      if (retry) {
        // we want to retry as many times as needed as long as the RS is not dead.
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("Retrying to same RegionServer %s because: %s",
              serverName, e.getMessage()), e);
        }
        submitTask(this);
        return true;
      }

      // trying to send the request elsewhere instead
      LOG.warn(String.format("the request should be tried elsewhere instead; server=%s try=%d",
                  serverName, numberOfAttemptsSoFar), e);
      return false;
    }

    private long getMaxWaitTime() {
      if (this.maxWaitTime < 0) {
        // This is the max attempts, not retries, so it should be at least 1.
        this.maxWaitTime = EnvironmentEdgeManager.currentTime() + rsStartupWaitTime;
      }
      return this.maxWaitTime;
    }

    protected IOException unwrapException(IOException e) {
      if (e instanceof RemoteException) {
        e = ((RemoteException)e).unwrapRemoteException();
      }
      return e;
    }
  }

  private interface RemoteProcedureResolver {
    void dispatchOpenRequests(MasterProcedureEnv env, List<RegionOpenOperation> operations);
    void dispatchCloseRequests(MasterProcedureEnv env, List<RegionCloseOperation> operations);
  }

  public void splitAndResolveOperation(final ServerName serverName,
      final Set<RemoteProcedure> operations, final RemoteProcedureResolver resolver) {
    final MasterProcedureEnv env = master.getMasterProcedureExecutor().getEnvironment();
    final ArrayListMultimap<Class<?>, RemoteOperation> reqsByType =
      buildAndGroupRequestByType(env, serverName, operations);

    final List<RegionOpenOperation> openOps = fetchType(reqsByType, RegionOpenOperation.class);
    if (!openOps.isEmpty()) resolver.dispatchOpenRequests(env, openOps);

    final List<RegionCloseOperation> closeOps = fetchType(reqsByType, RegionCloseOperation.class);
    if (!closeOps.isEmpty()) resolver.dispatchCloseRequests(env, closeOps);

    if (!reqsByType.isEmpty()) {
      LOG.warn("unknown request type in the queue: " + reqsByType);
    }
  }

  // ==========================================================================
  //  Compatibility calls
  // ==========================================================================
  protected class ExecuteProceduresRemoteCall extends AbstractRSRemoteCall
      implements RemoteProcedureResolver {
    private final Set<RemoteProcedure> operations;

    private ExecuteProceduresRequest.Builder request = null;

    public ExecuteProceduresRemoteCall(final ServerName serverName,
        final Set<RemoteProcedure> operations) {
      super(serverName);
      this.operations = operations;
    }

    public Void call() {
      final MasterProcedureEnv env = master.getMasterProcedureExecutor().getEnvironment();

      request = ExecuteProceduresRequest.newBuilder();
      if (LOG.isTraceEnabled()) {
        LOG.trace("Building request with operations count=" + operations.size());
      }
      splitAndResolveOperation(getServerName(), operations, this);

      try {
        final ExecuteProceduresResponse response = sendRequest(getServerName(), request.build());
        remoteCallCompleted(env, response);
      } catch (IOException e) {
        e = unwrapException(e);
        // TODO: In the future some operation may want to bail out early.
        // TODO: How many times should we retry (use numberOfAttemptsSoFar)
        if (!scheduleForRetry(e)) {
          remoteCallFailed(env, e);
        }
      }
      return null;
    }

    public void dispatchOpenRequests(final MasterProcedureEnv env,
        final List<RegionOpenOperation> operations) {
      request.addOpenRegion(buildOpenRegionRequest(env, getServerName(), operations));
    }

    public void dispatchCloseRequests(final MasterProcedureEnv env,
        final List<RegionCloseOperation> operations) {
      for (RegionCloseOperation op: operations) {
        request.addCloseRegion(op.buildCloseRegionRequest(getServerName()));
      }
    }

    protected ExecuteProceduresResponse sendRequest(final ServerName serverName,
        final ExecuteProceduresRequest request) throws IOException {
      try {
        return getRsAdmin().executeProcedures(null, request);
      } catch (ServiceException se) {
        throw ProtobufUtil.getRemoteException(se);
      }
    }


    private void remoteCallCompleted(final MasterProcedureEnv env,
        final ExecuteProceduresResponse response) {
      /*
      for (RemoteProcedure proc: operations) {
        proc.remoteCallCompleted(env, getServerName(), response);
      }*/
    }

    private void remoteCallFailed(final MasterProcedureEnv env, final IOException e) {
      for (RemoteProcedure proc: operations) {
        proc.remoteCallFailed(env, getServerName(), e);
      }
    }
  }

  // ==========================================================================
  //  Compatibility calls
  //  Since we don't have a "batch proc-exec" request on the target RS
  //  we have to chunk the requests by type and dispatch the specific request.
  // ==========================================================================
  private static OpenRegionRequest buildOpenRegionRequest(final MasterProcedureEnv env,
      final ServerName serverName, final List<RegionOpenOperation> operations) {
    final OpenRegionRequest.Builder builder = OpenRegionRequest.newBuilder();
    builder.setServerStartCode(serverName.getStartcode());
    builder.setMasterSystemTime(EnvironmentEdgeManager.currentTime());
    for (RegionOpenOperation op: operations) {
      builder.addOpenInfo(op.buildRegionOpenInfoRequest(env));
    }
    return builder.build();
  }

  private final class OpenRegionRemoteCall extends AbstractRSRemoteCall {
    private final List<RegionOpenOperation> operations;

    public OpenRegionRemoteCall(final ServerName serverName,
        final List<RegionOpenOperation> operations) {
      super(serverName);
      this.operations = operations;
    }

    @Override
    public Void call() {
      final MasterProcedureEnv env = master.getMasterProcedureExecutor().getEnvironment();
      final OpenRegionRequest request = buildOpenRegionRequest(env, getServerName(), operations);

      try {
        OpenRegionResponse response = sendRequest(getServerName(), request);
        remoteCallCompleted(env, response);
      } catch (IOException e) {
        e = unwrapException(e);
        // TODO: In the future some operation may want to bail out early.
        // TODO: How many times should we retry (use numberOfAttemptsSoFar)
        if (!scheduleForRetry(e)) {
          remoteCallFailed(env, e);
        }
      }
      return null;
    }

    private OpenRegionResponse sendRequest(final ServerName serverName,
        final OpenRegionRequest request) throws IOException {
      try {
        return getRsAdmin().openRegion(null, request);
      } catch (ServiceException se) {
        throw ProtobufUtil.getRemoteException(se);
      }
    }

    private void remoteCallCompleted(final MasterProcedureEnv env,
        final OpenRegionResponse response) {
      int index = 0;
      for (RegionOpenOperation op: operations) {
        OpenRegionResponse.RegionOpeningState state = response.getOpeningState(index++);
        op.setFailedOpen(state == OpenRegionResponse.RegionOpeningState.FAILED_OPENING);
        op.getRemoteProcedure().remoteCallCompleted(env, getServerName(), op);
      }
    }

    private void remoteCallFailed(final MasterProcedureEnv env, final IOException e) {
      for (RegionOpenOperation op: operations) {
        op.getRemoteProcedure().remoteCallFailed(env, getServerName(), e);
      }
    }
  }

  private final class CloseRegionRemoteCall extends AbstractRSRemoteCall {
    private final RegionCloseOperation operation;

    public CloseRegionRemoteCall(final ServerName serverName,
        final RegionCloseOperation operation) {
      super(serverName);
      this.operation = operation;
    }

    @Override
    public Void call() {
      final MasterProcedureEnv env = master.getMasterProcedureExecutor().getEnvironment();
      final CloseRegionRequest request = operation.buildCloseRegionRequest(getServerName());
      try {
        CloseRegionResponse response = sendRequest(getServerName(), request);
        remoteCallCompleted(env, response);
      } catch (IOException e) {
        e = unwrapException(e);
        // TODO: In the future some operation may want to bail out early.
        // TODO: How many times should we retry (use numberOfAttemptsSoFar)
        if (!scheduleForRetry(e)) {
          remoteCallFailed(env, e);
        }
      }
      return null;
    }

    private CloseRegionResponse sendRequest(final ServerName serverName,
        final CloseRegionRequest request) throws IOException {
      try {
        return getRsAdmin().closeRegion(null, request);
      } catch (ServiceException se) {
        throw ProtobufUtil.getRemoteException(se);
      }
    }

    private void remoteCallCompleted(final MasterProcedureEnv env,
        final CloseRegionResponse response) {
      operation.setClosed(response.getClosed());
      operation.getRemoteProcedure().remoteCallCompleted(env, getServerName(), operation);
    }

    private void remoteCallFailed(final MasterProcedureEnv env, final IOException e) {
      operation.getRemoteProcedure().remoteCallFailed(env, getServerName(), e);
    }
  }

  protected class CompatRemoteProcedureResolver implements Callable<Void>, RemoteProcedureResolver {
    private final Set<RemoteProcedure> operations;
    private final ServerName serverName;

    public CompatRemoteProcedureResolver(final ServerName serverName,
        final Set<RemoteProcedure> operations) {
      this.serverName = serverName;
      this.operations = operations;
    }

    @Override
    public Void call() {
      splitAndResolveOperation(serverName, operations, this);
      return null;
    }

    public void dispatchOpenRequests(final MasterProcedureEnv env,
        final List<RegionOpenOperation> operations) {
      submitTask(new OpenRegionRemoteCall(serverName, operations));
    }

    public void dispatchCloseRequests(final MasterProcedureEnv env,
        final List<RegionCloseOperation> operations) {
      for (RegionCloseOperation op: operations) {
        submitTask(new CloseRegionRemoteCall(serverName, op));
      }
    }
  }

  // ==========================================================================
  //  RPC Messages
  //  - ServerOperation: refreshConfig, grant, revoke, ...
  //  - RegionOperation: open, close, flush, snapshot, ...
  // ==========================================================================
  public static abstract class ServerOperation extends RemoteOperation {
    protected ServerOperation(final RemoteProcedure remoteProcedure) {
      super(remoteProcedure);
    }
  }

  public static abstract class RegionOperation extends RemoteOperation {
    private final HRegionInfo regionInfo;

    protected RegionOperation(final RemoteProcedure remoteProcedure,
        final HRegionInfo regionInfo) {
      super(remoteProcedure);
      this.regionInfo = regionInfo;
    }

    public HRegionInfo getRegionInfo() {
      return this.regionInfo;
    }
  }

  public static class RegionOpenOperation extends RegionOperation {
    private final List<ServerName> favoredNodes;
    private final boolean openForReplay;
    private boolean failedOpen;

    public RegionOpenOperation(final RemoteProcedure remoteProcedure,
        final HRegionInfo regionInfo, final List<ServerName> favoredNodes,
        final boolean openForReplay) {
      super(remoteProcedure, regionInfo);
      this.favoredNodes = favoredNodes;
      this.openForReplay = openForReplay;
    }

    protected void setFailedOpen(final boolean failedOpen) {
      this.failedOpen = failedOpen;
    }

    public boolean isFailedOpen() {
      return failedOpen;
    }

    public OpenRegionRequest.RegionOpenInfo buildRegionOpenInfoRequest(
        final MasterProcedureEnv env) {
      return RequestConverter.buildRegionOpenInfo(getRegionInfo(),
        env.getAssignmentManager().getFavoredNodes(getRegionInfo()), false);
    }
  }

  public static class RegionCloseOperation extends RegionOperation {
    private final ServerName destinationServer;
    private boolean closed = false;

    public RegionCloseOperation(final RemoteProcedure remoteProcedure,
        final HRegionInfo regionInfo, final ServerName destinationServer) {
      super(remoteProcedure, regionInfo);
      this.destinationServer = destinationServer;
    }

    public ServerName getDestinationServer() {
      return destinationServer;
    }

    protected void setClosed(final boolean closed) {
      this.closed = closed;
    }

    public boolean isClosed() {
      return closed;
    }

    public CloseRegionRequest buildCloseRegionRequest(final ServerName serverName) {
      return ProtobufUtil.buildCloseRegionRequest(serverName,
        getRegionInfo().getRegionName(), getDestinationServer());
    }
  }
}
