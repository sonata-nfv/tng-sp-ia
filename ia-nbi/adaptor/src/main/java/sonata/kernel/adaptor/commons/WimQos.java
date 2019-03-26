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
 * @author Dario Valocchi (Ph.D.)
 * 
 */

package sonata.kernel.adaptor.commons;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WimQos {

  @JsonProperty("node_1")
  private String node1;
  @JsonProperty("node_2")
  private String node2;
  @JsonProperty("latency")
  private String latency;
  @JsonProperty("latency_unit")
  private String latencyUnit;
  @JsonProperty("bandwidth")
  private String bandwidth;
  @JsonProperty("bandwidth_unit")
  private String bandwidthUnit;


  public String getNode1() {
    return node1;
  }

  public String getNode2() {
    return node2;
  }

  public String getLatency() {
    return latency;
  }

  public String getLatencyUnit() {
    return latencyUnit;
  }

  public String getBandwidth() {
    return bandwidth;
  }

  public String getBandwidthUnit() {
    return bandwidthUnit;
  }


  public void setNode1(String node1) {
    this.node1 = node1;
  }

  public void setNode2(String node2) {
    this.node2 = node2;
  }

  public void setLatency(String latency) {
    this.latency = latency;
  }

  public void setLatencyUnit(String latencyUnit) {
    this.latencyUnit = latencyUnit;
  }

  public void setBandwidth(String bandwidth) {
    this.bandwidth = bandwidth;
  }

  public void setBandwidthUnit(String bandwidthUnit) {
    this.bandwidthUnit = bandwidthUnit;
  }

}
