/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */
package org.apache.axis2.clustering;

import java.util.Properties;

/**
 * Represents a member in the cluster. This is used with static membership
 */
public class Member {

    /**
     * The host name of this member. Can be the name or the IP address
     */
    private String hostName;

    /**
     * The TCP port used by this member for communicating clustering messages
     */
    private int port = -1;

    /**
     * The HTTP port used by this member for servicing Web service requests. Used for load balancing
     */
    private int httpPort = -1;

    /**
     * The HTTPS port used by this member for servicing Web service requests. Used for load balancing
     */
    private int httpsPort = -1;

    /**
     * Indicates whether this member is ACTIVE or PASSIVE
     */
    private boolean isActive = true;

    /**
     * The domain of this member
     */
    private String domain;

    /**
     * Other member specific properties
     */
    private Properties properties = new Properties();

    /**
     * Time at which this member was suspended
     */
    private long suspendedTime = -1;

    /**
     * Time in millis which this member should be suspended
     */
    private long suspensionDuration = -1;

    public Member(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    /**
     * Temporarilly suspend this member
     * @param suspensionDurationMillis The time duration in millis in which this member should be suspended
     */
    public void suspend(long suspensionDurationMillis){
        this.suspendedTime = System.currentTimeMillis();
        this.suspensionDuration = suspensionDurationMillis;
    }

    /**
     * Check whether this member is suspended
     * @return true if this member is still suspended, false otherwise
     */
    public boolean isSuspended() {
        if (suspendedTime == -1) {
            return false;
        }
        if (System.currentTimeMillis() - suspendedTime >= suspensionDuration) {
            this.suspendedTime = -1;
            this.suspensionDuration = -1;
            return false;
        }
        return true;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Member member = (Member) o;
        return port == member.getPort() &&
               httpPort == member.getHttpPort() &&
               httpsPort == member.getHttpsPort() &&
               !(hostName != null ? !hostName.equals(member.getHostName()) :
                 member.getHostName() != null);
    }

    public int hashCode() {
        int result;
        result = (hostName != null ? hostName.hashCode() : 0);
        result = 31 * result + port;
        return result;
    }

    public String toString() {
        return "Host:" + hostName + ", Port: " + port +
               ", HTTP:" + httpPort + ", HTTPS:" + httpsPort +
               ", Domain: " + domain + ", Sub-domain:" + properties.getProperty("subDomain") +
               ", Active:" + isActive;
    }
}
