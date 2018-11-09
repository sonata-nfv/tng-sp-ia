/*
 * Copyright (c) 2015 SONATA-NFV, UCL, NOKIA, NCSR Demokritos ALL RIGHTS RESERVED.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Neither the name of the SONATA-NFV, UCL, NOKIA, NCSR Demokritos nor the names of its contributors
 * may be used to endorse or promote products derived from this software without specific prior
 * written permission.
 * 
 * This work has been performed in the framework of the SONATA project, funded by the European
 * Commission under Grant number 671517 through the Horizon 2020 and 5G-PPP programmes. The authors
 * would like to acknowledge the contributions of their colleagues of the SONATA partner consortium
 * (www.sonata-nfv.eu).
 *
 * @author Dario Valocchi (Ph.D.), UCL
 * 
 */

package sonata.kernel.adaptor.commons.nsd;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class ConnectionPoint {


  private String id;
  @JsonProperty("interface")
  private InterfaceType interfaceTye;
  private ConnectionPointType type;
  private String mac;
  private String ip;
  private String qos;
  @JsonProperty("security_groups")
  private ArrayList<String> securityGroups;
  @JsonProperty("virtua_link_reference")
  private String virtualLinkReference;


  public String getId() {
    return id;
  }

  public ConnectionPointType getType() {
    return type;
  }

  public String getMac() {
    return mac;
  }

  public String getIp() {
    return ip;
  }

  public String getQos() {
    return qos;
  }

  public ArrayList<String> getSecurityGroups() {
    return securityGroups;
  }

  public String getVirtualLinkReference() {
    return virtualLinkReference;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setType(ConnectionPointType type) {
    this.type = type;
  }

  public void setMac(String mac) {
    this.mac = mac;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public void setQos(String qos) {
    this.qos = qos;
  }

  public void setSecurityGroups(ArrayList<String> securityGroups) {
    this.securityGroups = securityGroups;
  }

  public void setVirtualLinkReference(String virtualLinkReference) {
    this.virtualLinkReference = virtualLinkReference;
  }
}
