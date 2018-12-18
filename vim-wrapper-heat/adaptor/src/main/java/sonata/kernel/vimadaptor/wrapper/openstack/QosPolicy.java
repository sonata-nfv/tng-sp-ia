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

import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class QosPolicy implements Comparable<QosPolicy> {

  private static final org.slf4j.Logger Logger =
          LoggerFactory.getLogger(QosPolicy.class);

  private String qosPolicyName;

  private String id;

  private ArrayList<QosRule> qosRules;

  /**
   * Basic flavor constructor.
   *
   * @param qosPolicyName the name of this Qos Policy
   * @param qosRules the Qos Rules Array

   */
  public QosPolicy(String qosPolicyName, ArrayList<QosRule> qosRules) {
    super();
    this.qosPolicyName = qosPolicyName;
    this.qosRules = qosRules;

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(QosPolicy other) {

    if ((this.getQosRulesByTypeAndDirection("minimum_bandwidth","egress")
            - other.getQosRulesByTypeAndDirection("minimum_bandwidth","egress")) != 0)
      return (int) Math.signum(this.getQosRulesByTypeAndDirection("minimum_bandwidth","egress")
              - other.getQosRulesByTypeAndDirection("minimum_bandwidth","egress") );
    else if ((this.getQosRulesByTypeAndDirection("bandwidth_limit","egress")
             - other.getQosRulesByTypeAndDirection("bandwidth_limit","egress")) != 0)
      return (int) Math.signum(this.getQosRulesByTypeAndDirection("bandwidth_limit","egress")
              - other.getQosRulesByTypeAndDirection("bandwidth_limit","egress"));
    else
      return 0;
  }

  public int getQosRulesByTypeAndDirection(String type, String direction) {

    for (QosRule qosRule : qosRules) {
      if (!qosRule.getType().equals(type)) continue;
      else if (qosRule.getDirection().equals(direction)) {
        if (type.equals("bandwidth_limit")) return qosRule.getMaxKbps();
        if (type.equals("minimum_bandwidth")) return qosRule.getMinKbps();
      }
    }
    return 0;
  }


  public String getQosPolicyName() {
    return qosPolicyName;
  }

  public String getId() {
    return id;
  }

  public ArrayList<QosRule> getQosRules() {
    return qosRules;
  }

  public void setQosPolicyName(String qosPolicyName) {
    this.qosPolicyName = qosPolicyName;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setQosRules(ArrayList<QosRule> qosRules) {
    this.qosRules = qosRules;
  }

}
