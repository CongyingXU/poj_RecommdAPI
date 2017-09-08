/*
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

package org.apache.axis2.util;

import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;

import java.util.ArrayList;
import java.net.URL;

public class FaultyServiceData {

    private String serviceGroupName;
    private AxisServiceGroup serviceGroup;
    private ArrayList<AxisService> serviceList;
    private DeploymentFileData currentDeploymentFile;
    private URL serviceLocation;

    public FaultyServiceData(AxisServiceGroup serviceGroup,
                             ArrayList<AxisService> serviceList,
                             URL serviceLocation,
                             DeploymentFileData currentDeploymentFile) {
        serviceGroupName = serviceGroup.getServiceGroupName();
        this.serviceGroup = serviceGroup;
        this.serviceList = serviceList;
        this.currentDeploymentFile = currentDeploymentFile;
        this.serviceLocation = serviceLocation;

    }

    public AxisServiceGroup getServiceGroup() {
        return serviceGroup;
    }

    public DeploymentFileData getCurrentDeploymentFile() {
        return currentDeploymentFile;
    }

    public ArrayList<AxisService> getServiceList() {
        return serviceList;
    }

    public URL getServiceLocation() {
        return serviceLocation;
    }

    public String getServiceGroupName() {
        return serviceGroupName;
    }
}
