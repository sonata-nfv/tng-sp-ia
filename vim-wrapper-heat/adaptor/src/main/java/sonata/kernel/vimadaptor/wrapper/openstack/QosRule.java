/*
 * Copyright (c) 2015 SONATA-NFV, UCL, NOKIA, THALES, NCSR Demokritos ALL RIGHTS RESERVED.
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
 * Neither the name of the SONATA-NFV, UCL, NOKIA, THALES, NCSR Demokritos nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This work has been performed in the framework of the SONATA project, funded by the European
 * Commission under Grant number 671517 through the Horizon 2020 and 5G-PPP programmes. The authors
 * would like to acknowledge the contributions of their colleagues of the SONATA partner consortium
 * (www.sonata-nfv.eu).
 *
 * @author Bruno Vidalenc (Ph.D.), Thales
 * 
 * @author Dario Valocchi (Ph.D.), UCL
 * 
 */

package sonata.kernel.vimadaptor.wrapper.openstack;


public class QosRule {


  private String id;
  private String type;
  private String direction;
  private int maxKbps;
  private int minKbps;

  /**
   * Basic flavor constructor.
   *
   * @param id the id of this Qos Rule
   * @param type the type of this Qos Rule (bandwidth_limit or minimum_bandwidth)
   * @param direction the direction of this Qos Rule (egress or ingress)
   * @param maxKbps the max bandwidth kbps of this Qos Rule
   * @param minKbps the min bandwidth kbps of this Qos Rule
   */
  public QosRule(String id, String type, String direction, int maxKbps, int minKbps) {
    super();
    this.id = id;
    this.type = type;
    this.direction = direction;
    this.maxKbps = maxKbps;
    this.minKbps = minKbps;

  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getDirection() {
    return direction;
  }

  public int getMaxKbps() {
    return maxKbps;
  }

  public int getMinKbps() {
    return minKbps;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public void setMaxKbps(int maxKbps) {
    this.maxKbps = maxKbps;
  }

  public void setMinKbps(int minKbps) {
    this.minKbps = minKbps;
  }

}
