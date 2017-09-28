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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.RegionReplicaUtil;
import org.apache.hadoop.hbase.master.MasterServices;
import org.apache.hadoop.hbase.master.MasterWalManager;
import org.apache.hadoop.hbase.master.assignment.AssignProcedure;
import org.apache.hadoop.hbase.master.assignment.AssignmentManager;
import org.apache.hadoop.hbase.master.assignment.RegionTransitionProcedure;
import org.apache.hadoop.hbase.procedure2.ProcedureMetrics;
import org.apache.hadoop.hbase.procedure2.ProcedureSuspendedException;
import org.apache.hadoop.hbase.procedure2.ProcedureYieldException;
import org.apache.hadoop.hbase.procedure2.StateMachineProcedure;
import org.apache.hadoop.hbase.shaded.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.shaded.protobuf.generated.HBaseProtos.RegionInfo;
import org.apache.hadoop.hbase.shaded.protobuf.generated.MasterProcedureProtos;
import org.apache.hadoop.hbase.shaded.protobuf.generated.MasterProcedureProtos.ServerCrashState;

/**
 * Handle crashed server. This is a port to ProcedureV2 of what used to be euphemistically called
 * ServerShutdownHandler.
 *
 * <p>The procedure flow varies dependent on whether meta is assigned and if we are to split logs.
 *
 * <p>We come in here after ServerManager has noticed a server has expired. Procedures
 * queued on the rpc should have been notified about fail and should be concurrently
 * getting themselves ready to assign elsewhere.
 */
public class ServerCrashProcedure
extends StateMachineProcedure<MasterProcedureEnv, ServerCrashState>
implements ServerProcedureInterface {
  private static final Log LOG = LogFactory.getLog(ServerCrashProcedure.class);

  /**
   * Name of the crashed server to process.
   */
  private ServerName serverName;

  /**
   * Whether DeadServer knows that we are processing it.
   */
  private boolean notifiedDeadServer = false;

  /**
   * Regions that were on the crashed server.
   */
  private List<HRegionInfo> regionsOnCrashedServer;

  private boolean carryingMeta = false;
  private boolean shouldSplitWal;

  /**
   * Cycles on same state. Good for figuring if we are stuck.
   */
  private int cycles = 0;

  /**
   * Ordinal of the previous state. So we can tell if we are progressing or not. TODO: if useful,
   * move this back up into StateMachineProcedure
   */
  private int previousState;

  /**
   * Call this constructor queuing up a Procedure.
   * @param serverName Name of the crashed server.
   * @param shouldSplitWal True if we should split WALs as part of crashed server processing.
   * @param carryingMeta True if carrying hbase:meta table region.
   */
  public ServerCrashProcedure(
      final MasterProcedureEnv env,
      final ServerName serverName,
      final boolean shouldSplitWal,
      final boolean carryingMeta) {
    this.serverName = serverName;
    this.shouldSplitWal = shouldSplitWal;
    this.carryingMeta = carryingMeta;
    this.setOwner(env.getRequestUser());
  }

  /**
   * Used when deserializing from a procedure store; we'll construct one of these then call
   * {@link #deserializeStateData(InputStream)}. Do not use directly.
   */
  public ServerCrashProcedure() {
    super();
  }

  @Override
  protected Flow executeFromState(MasterProcedureEnv env, ServerCrashState state)
      throws ProcedureSuspendedException, ProcedureYieldException {
    if (LOG.isTraceEnabled()) {
      LOG.trace(state  + " " + this + "; cycles=" + this.cycles);
    }
    // Keep running count of cycles
    if (state.ordinal() != this.previousState) {
      this.previousState = state.ordinal();
      this.cycles = 0;
    } else {
      this.cycles++;
    }
    final MasterServices services = env.getMasterServices();
    // HBASE-14802
    // If we have not yet notified that we are processing a dead server, we should do now.
    if (!notifiedDeadServer) {
      services.getServerManager().getDeadServers().notifyServer(serverName);
      notifiedDeadServer = true;
    }

    try {
      switch (state) {
      case SERVER_CRASH_START:
        LOG.info("Start " + this);
        start(env);
        // If carrying meta, process it first. Else, get list of regions on crashed server.
        if (this.carryingMeta) {
          setNextState(ServerCrashState.SERVER_CRASH_PROCESS_META);
        } else {
          setNextState(ServerCrashState.SERVER_CRASH_GET_REGIONS);
        }
        break;

      case SERVER_CRASH_GET_REGIONS:
        // If hbase:meta is not assigned, yield.
        if (env.getAssignmentManager().waitMetaLoaded(this)) {
          throw new ProcedureSuspendedException();
        }

        this.regionsOnCrashedServer = services.getAssignmentManager().getRegionStates()
          .getServerRegionInfoSet(serverName);
        // Where to go next? Depends on whether we should split logs at all or
        // if we should do distributed log splitting.
        if (!this.shouldSplitWal) {
          setNextState(ServerCrashState.SERVER_CRASH_ASSIGN);
        } else {
          setNextState(ServerCrashState.SERVER_CRASH_SPLIT_LOGS);
        }
        break;

      case SERVER_CRASH_PROCESS_META:
        processMeta(env);
        setNextState(ServerCrashState.SERVER_CRASH_GET_REGIONS);
        break;

      case SERVER_CRASH_SPLIT_LOGS:
        splitLogs(env);
        setNextState(ServerCrashState.SERVER_CRASH_ASSIGN);
        break;

      case SERVER_CRASH_ASSIGN:
        // If no regions to assign, skip assign and skip to the finish.
        // Filter out meta regions. Those are handled elsewhere in this procedure.
        // Filter changes this.regionsOnCrashedServer.
        if (filterDefaultMetaRegions(regionsOnCrashedServer)) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("Assigning regions " +
              HRegionInfo.getShortNameToLog(regionsOnCrashedServer) + ", " + this +
              "; cycles=" + this.cycles);
          }
          handleRIT(env, regionsOnCrashedServer);
          AssignmentManager am = env.getAssignmentManager();
          addChildProcedure(am.
              createAssignProcedures(am.getOrderedRegions(regionsOnCrashedServer), true));
        }
        setNextState(ServerCrashState.SERVER_CRASH_FINISH);
        break;

      case SERVER_CRASH_FINISH:
        services.getServerManager().getDeadServers().finish(serverName);
        return Flow.NO_MORE_STATE;

      default:
        throw new UnsupportedOperationException("unhandled state=" + state);
      }
    } catch (IOException e) {
      LOG.warn("Failed state=" + state + ", retry " + this + "; cycles=" + this.cycles, e);
    }
    return Flow.HAS_MORE_STATE;
  }

  /**
   * Start processing of crashed server. In here we'll just set configs. and return.
   * @param env
   * @throws IOException
   */
  private void start(final MasterProcedureEnv env) throws IOException {}

  /**
   * @param env
   * @throws IOException
   * @throws InterruptedException
   */
  private void processMeta(final MasterProcedureEnv env) throws IOException {
    if (LOG.isDebugEnabled()) LOG.debug("Processing hbase:meta that was on " + this.serverName);

    if (this.shouldSplitWal) {
      // TODO: Matteo. We BLOCK here but most important thing to be doing at this moment.
      env.getMasterServices().getMasterWalManager().splitMetaLog(serverName);
    }

    // Assign meta if still carrying it. Check again: region may be assigned because of RIT timeout
    final AssignmentManager am = env.getMasterServices().getAssignmentManager();
    for (HRegionInfo hri: am.getRegionStates().getServerRegionInfoSet(serverName)) {
      if (!isDefaultMetaRegion(hri)) continue;

      am.offlineRegion(hri);
      addChildProcedure(am.createAssignProcedure(hri, true));
    }
  }

  private boolean filterDefaultMetaRegions(final List<HRegionInfo> regions) {
    if (regions == null) return false;
    final Iterator<HRegionInfo> it = regions.iterator();
    while (it.hasNext()) {
      final HRegionInfo hri = it.next();
      if (isDefaultMetaRegion(hri)) {
        it.remove();
      }
    }
    return !regions.isEmpty();
  }

  private boolean isDefaultMetaRegion(final HRegionInfo hri) {
    return hri.getTable().equals(TableName.META_TABLE_NAME) &&
      RegionReplicaUtil.isDefaultReplica(hri);
  }

  private void splitLogs(final MasterProcedureEnv env) throws IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Splitting WALs " + this);
    }
    MasterWalManager mwm = env.getMasterServices().getMasterWalManager();
    AssignmentManager am = env.getMasterServices().getAssignmentManager();
    // TODO: For Matteo. Below BLOCKs!!!! Redo so can relinquish executor while it is running.
    // PROBLEM!!! WE BLOCK HERE.
    mwm.splitLog(this.serverName);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Done splitting WALs " + this);
    }
    am.getRegionStates().logSplit(this.serverName);
  }

  static int size(final Collection<HRegionInfo> hris) {
    return hris == null? 0: hris.size();
  }

  @Override
  protected void rollbackState(MasterProcedureEnv env, ServerCrashState state)
  throws IOException {
    // Can't rollback.
    throw new UnsupportedOperationException("unhandled state=" + state);
  }

  @Override
  protected ServerCrashState getState(int stateId) {
    return ServerCrashState.valueOf(stateId);
  }

  @Override
  protected int getStateId(ServerCrashState state) {
    return state.getNumber();
  }

  @Override
  protected ServerCrashState getInitialState() {
    return ServerCrashState.SERVER_CRASH_START;
  }

  @Override
  protected boolean abort(MasterProcedureEnv env) {
    // TODO
    return false;
  }

  @Override
  protected LockState acquireLock(final MasterProcedureEnv env) {
    // TODO: Put this BACK AFTER AMv2 goes in!!!!
    // if (env.waitFailoverCleanup(this)) return LockState.LOCK_EVENT_WAIT;
    if (env.waitServerCrashProcessingEnabled(this)) return LockState.LOCK_EVENT_WAIT;
    if (env.getProcedureScheduler().waitServerExclusiveLock(this, getServerName())) {
      return LockState.LOCK_EVENT_WAIT;
    }
    return LockState.LOCK_ACQUIRED;
  }

  @Override
  protected void releaseLock(final MasterProcedureEnv env) {
    env.getProcedureScheduler().wakeServerExclusiveLock(this, getServerName());
  }

  @Override
  public void toStringClassDetails(StringBuilder sb) {
    sb.append(getClass().getSimpleName());
    sb.append(" server=");
    sb.append(serverName);
    sb.append(", splitWal=");
    sb.append(shouldSplitWal);
    sb.append(", meta=");
    sb.append(carryingMeta);
  }

  @Override
  public void serializeStateData(final OutputStream stream) throws IOException {
    super.serializeStateData(stream);

    MasterProcedureProtos.ServerCrashStateData.Builder state =
      MasterProcedureProtos.ServerCrashStateData.newBuilder().
      setServerName(ProtobufUtil.toServerName(this.serverName)).
      setCarryingMeta(this.carryingMeta).
      setShouldSplitWal(this.shouldSplitWal);
    if (this.regionsOnCrashedServer != null && !this.regionsOnCrashedServer.isEmpty()) {
      for (HRegionInfo hri: this.regionsOnCrashedServer) {
        state.addRegionsOnCrashedServer(HRegionInfo.convert(hri));
      }
    }
    state.build().writeDelimitedTo(stream);
  }

  @Override
  public void deserializeStateData(final InputStream stream) throws IOException {
    super.deserializeStateData(stream);

    MasterProcedureProtos.ServerCrashStateData state =
      MasterProcedureProtos.ServerCrashStateData.parseDelimitedFrom(stream);
    this.serverName = ProtobufUtil.toServerName(state.getServerName());
    this.carryingMeta = state.hasCarryingMeta()? state.getCarryingMeta(): false;
    // shouldSplitWAL has a default over in pb so this invocation will always work.
    this.shouldSplitWal = state.getShouldSplitWal();
    int size = state.getRegionsOnCrashedServerCount();
    if (size > 0) {
      this.regionsOnCrashedServer = new ArrayList<HRegionInfo>(size);
      for (RegionInfo ri: state.getRegionsOnCrashedServerList()) {
        this.regionsOnCrashedServer.add(HRegionInfo.convert(ri));
      }
    }
  }

  @Override
  public ServerName getServerName() {
    return this.serverName;
  }

  @Override
  public boolean hasMetaTableRegion() {
    return this.carryingMeta;
  }

  @Override
  public ServerOperationType getServerOperationType() {
    return ServerOperationType.CRASH_HANDLER;
  }

  /**
   * For this procedure, yield at end of each successful flow step so that all crashed servers
   * can make progress rather than do the default which has each procedure running to completion
   * before we move to the next. For crashed servers, especially if running with distributed log
   * replay, we will want all servers to come along; we do not want the scenario where a server is
   * stuck waiting for regions to online so it can replay edits.
   */
  @Override
  protected boolean isYieldBeforeExecuteFromState(MasterProcedureEnv env, ServerCrashState state) {
    return true;
  }

  @Override
  protected boolean shouldWaitClientAck(MasterProcedureEnv env) {
    // The operation is triggered internally on the server
    // the client does not know about this procedure.
    return false;
  }

  /**
   * Handle any outstanding RIT that are up against this.serverName, the crashed server.
   * Notify them of crash. Remove assign entries from the passed in <code>regions</code>
   * otherwise we have two assigns going on and they will fight over who has lock.
   * Notify Unassigns also.
   * @param crashedServer Server that crashed.
   * @param regions Regions that were on crashed server
   * @return Subset of <code>regions</code> that were RIT against <code>crashedServer</code>
   */
  private void handleRIT(final MasterProcedureEnv env, final List<HRegionInfo> regions) {
    if (regions == null) return;
    AssignmentManager am = env.getMasterServices().getAssignmentManager();
    final Iterator<HRegionInfo> it = regions.iterator();
    ServerCrashException sce = null;
    while (it.hasNext()) {
      final HRegionInfo hri = it.next();
      RegionTransitionProcedure rtp = am.getRegionStates().getRegionTransitionProcedure(hri);
      if (rtp == null) continue;
      // Make sure the RIT is against this crashed server. In the case where there are many
      // processings of a crashed server -- backed up for whatever reason (slow WAL split) --
      // then a previous SCP may have already failed an assign, etc., and it may have a new
      // location target; DO NOT fail these else we make for assign flux.
      ServerName rtpServerName = rtp.getServer(env);
      if (rtpServerName == null) {
        LOG.warn("RIT with ServerName null! " + rtp);
        continue;
      }
      if (!rtpServerName.equals(this.serverName)) continue;
      LOG.info("pid=" + getProcId() + " found RIT " + rtp + "; " +
        rtp.getRegionState(env).toShortString());
      // Notify RIT on server crash.
      if (sce == null) {
        sce = new ServerCrashException(getProcId(), getServerName());
      }
      rtp.remoteCallFailed(env, this.serverName, sce);
      if (rtp instanceof AssignProcedure) {
        // If an assign, include it in our return and remove from passed-in list of regions.
        it.remove();
      }
    }
  }

  @Override
  protected ProcedureMetrics getProcedureMetrics(MasterProcedureEnv env) {
    return env.getMasterServices().getMasterMetrics().getServerCrashProcMetrics();
  }
}
