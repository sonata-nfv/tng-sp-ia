/*
 * Copyright (c) 2015 SONATA-NFV, UCL, NOKIA, THALES, NCSR Demokritos ALL RIGHTS RESERVED. <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at <p>
 * http://www.apache.org/licenses/LICENSE-2.0 <p> Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License. <p> Neither the name of the
 * SONATA-NFV, UCL, NOKIA, THALES NCSR Demokritos nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * <p> This work has been performed in the framework of the SONATA project, funded by the European
 * Commission under Grant number 671517 through the Horizon 2020 and 5G-PPP programmes. The authors
 * would like to acknowledge the contributions of their colleagues of the SONATA partner consortium
 * (www.sonata-nfv.eu).
 *
 * @author Dario Valocchi (Ph.D.), UCL
 * 
 * @author Bruno Vidalenc (Ph.D.), THALES
 */

package sonata.kernel.vimadaptor.wrapper.openstack;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import sonata.kernel.vimadaptor.wrapper.openstack.javastackclient.JavaStackCore;
import sonata.kernel.vimadaptor.wrapper.openstack.javastackclient.JavaStackUtils;
import sonata.kernel.vimadaptor.wrapper.openstack.javastackclient.models.network.*;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This class wraps a Nova Client written in python when instantiated the onnection details of the
 * OpenStack instance should be provided.
 *
 */
public class OpenStackNeutronClient {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(OpenStackNeutronClient.class);

  private JavaStackCore javaStack; // instance for calling OpenStack APIs

  private ObjectMapper mapper;

  /**
   * Construct a new Openstack Neutron Client.
   *
   * @param url of the OpenStack endpoint
   * @param userName to log into the OpenStack service
   * @param password to log into the OpenStack service
   * @param domain to log into the OpenStack service
   * @param tenantName to log into the OpenStack service
   * @throws IOException if the authentication process fails
   */
  public OpenStackNeutronClient(String url, String userName, String password, String domain, String tenantName,
                                String identityPort) throws IOException {
    Logger.debug(
        "URL:" + url + "|User:" + userName + "|Project:" + tenantName + "|Pass:" + password + "|Domain:" + domain + "|");

    javaStack = JavaStackCore.getJavaStackCore();

    javaStack.setEndpoint(url);
    javaStack.setUsername(userName);
    javaStack.setPassword(password);
    javaStack.setDomain(domain);
    javaStack.setProjectName(tenantName);
    javaStack.setProjectId(null);
    javaStack.setAuthenticated(false);

    javaStack.authenticateClientV3(identityPort);

  }

  /**
   * Get the Qos Policies.
   *
   * @return the Qos Policies
   */
  public ArrayList<QosPolicy> getPolicies() {

    QosPolicy output_policy = null;
    String policyName = null;
    int cpu, ram, disk;

    ArrayList<QosPolicy> output_policies = new ArrayList<>();
    Logger.info("Getting qos policies");
    try {
      mapper = new ObjectMapper();
      String listPolicies =
          JavaStackUtils.convertHttpResponseToString(javaStack.listQosPolicies());
      Logger.info(listPolicies);
      PoliciesData inputPolicies = mapper.readValue(listPolicies, PoliciesData.class);
      Logger.info(inputPolicies.getPolicies().toString());
      for (PolicyProperties input_policy : inputPolicies.getPolicies()) {
        Logger.info(input_policy.getId() + ": " + input_policy.getName());

        policyName = input_policy.getName();
        ArrayList<QosRule> qosRules = new ArrayList<>();
        for (RulesProperties input_rule : input_policy.getRules()) {
          qosRules.add(new QosRule(input_rule.getId(),input_rule.getType(),input_rule.getDirection(),
                  input_rule.getMaxKbps(),input_rule.getMinKbps()));
        }

        output_policy = new QosPolicy(policyName, qosRules);
        output_policies.add(output_policy);
      }

    } catch (Exception e) {
      Logger.warn("Warning: Runtime error getting openstack qos policies" + " error message: " + e.getMessage());
    }

    return output_policies;

  }

  /**
   * Get the External Networks.
   *
   * @return the External Networks
   */
  public ArrayList<ExtNetwork> getNetworks() {

    ExtNetwork output_network = null;

    ArrayList<ExtNetwork> output_networks = new ArrayList<>();
    Logger.info("Getting external networks");
    try {
      mapper = new ObjectMapper();
      String listNetworks =
          JavaStackUtils.convertHttpResponseToString(javaStack.listNetworks());
      Logger.info(listNetworks);
      NetworksData inputNetworks = mapper.readValue(listNetworks, NetworksData.class);
      Logger.info(inputNetworks.getNetworks().toString());
      for (NetworksProperties input_network : inputNetworks.getNetworks()) {
        Logger.info(input_network.getId() + ": " + input_network.getName());

        output_network = new ExtNetwork(input_network.getName(), input_network.getId());
        output_networks.add(output_network);
      }

    } catch (Exception e) {
      Logger.error("Runtime error getting openstack external networks" + " error message: " + e.getMessage());
    }

    return output_networks;

  }

  /**
   * Get the Routers for a specific External Network.
   *
   * @return the Routers
   */
  public ArrayList<Router> getRouters(String network) {

    Router output_router = null;

    ArrayList<Router> output_routers = new ArrayList<>();
    Logger.info("Getting routers");
    try {
      mapper = new ObjectMapper();
      String listRouters =
          JavaStackUtils.convertHttpResponseToString(javaStack.listRouters());
      Logger.info(listRouters);
      RoutersData inputRouters = mapper.readValue(listRouters, RoutersData.class);
      Logger.info(inputRouters.getRouters().toString());
      for (RoutersProperties input_router : inputRouters.getRouters()) {
        if (input_router.getExternalGatewayInfo() != null) {
          Logger.info(input_router.getId() + ": " + input_router.getName() + ": " + input_router.getExternalGatewayInfo().getNetworkId());

          if (input_router.getExternalGatewayInfo().getNetworkId().equals(network)) {
            output_router = new Router(input_router.getName(), input_router.getId());
            output_routers.add(output_router);
          }
        }
      }

    } catch (Exception e) {
      Logger.error("Runtime error getting openstack routers" + " error message: " + e.getMessage());
    }

    if (output_routers.isEmpty()) {
      return null;
    } else {
      return output_routers;
    }

  }

  /**
   * Get the Subnet Pools
   *
   * @return the Subnet Pools
   */
  public ArrayList<SubnetPool> getSubnetPools() {

    SubnetPool outputSubnetPool = null;

    ArrayList<SubnetPool> outputSubnetPools = new ArrayList<>();
    Logger.info("Getting subnet pools");
    try {
      mapper = new ObjectMapper();
      String listSubnetPools =
          JavaStackUtils.convertHttpResponseToString(javaStack.listSubnetPools());
      Logger.info(listSubnetPools);
      SubnetPoolsData inputSubnetPools = mapper.readValue(listSubnetPools, SubnetPoolsData.class);
      Logger.info(inputSubnetPools.getSubnetPools().toString());
      for (SubnetPoolProperties inputSubnetPool : inputSubnetPools.getSubnetPools()) {
        Logger.info(inputSubnetPool.getId() + ": " + inputSubnetPool.getName());

        outputSubnetPool = new SubnetPool(inputSubnetPool.getName(), inputSubnetPool.getId(), inputSubnetPool.getPrefixes());
        outputSubnetPools.add(outputSubnetPool);
      }

    } catch (Exception e) {
      Logger.error("Runtime error getting openstack subnet pools" + " error message: " + e.getMessage());
    }

    return outputSubnetPools;

  }

  /**
   * Create a Subnet Pool
   *
   * @param name - the name
   * @param prefixes - A list of subnet prefixes to assign to the subnet pool
   * @param defaultPrefixlen - The size of the prefix to allocate when you create the subnet
   * @return - the uuid of the created subnet pool, if the process failed the returned value is null
   */
  public String createSubnetPool(String name, ArrayList<String> prefixes, String defaultPrefixlen) {
    String uuid = null;

    Logger.info("Creating subnet pool: " + name);

    try {
      mapper = new ObjectMapper();
      String createSubnetPoolResponse =
          JavaStackUtils.convertHttpResponseToString(javaStack.createSubnetPool(name,prefixes,defaultPrefixlen));
      Logger.info(createSubnetPoolResponse);
      SubnetPoolData inputSubnetPool = mapper.readValue(createSubnetPoolResponse, SubnetPoolData.class);
       uuid = inputSubnetPool.getSubnetPool().getId();

    } catch (Exception e) {
      Logger.error(
          "Runtime error creating subnet pool : " + name + " error message: " + e.getMessage());
      return null;
    }

    return uuid;
  }

  /**
   * Update a Subnet Pool
   *
   * @param id - the id
   * @param prefixes - A list of subnet prefixes to assign to the subnet pool
   * @return - the uuid of the created subnet pool, if the process failed the returned value is null
   */
  public String updateSubnetPool(String id, ArrayList<String> prefixes) {
    String uuid = null;

    Logger.info("Updating subnet pool id: " + id);

    try {
      mapper = new ObjectMapper();
      String updateSubnetPoolResponse =
          JavaStackUtils.convertHttpResponseToString(javaStack.updateSubnetPool(id,prefixes));
      Logger.info(updateSubnetPoolResponse);
      SubnetPoolData inputSubnetPool = mapper.readValue(updateSubnetPoolResponse, SubnetPoolData.class);
      uuid = inputSubnetPool.getSubnetPool().getId();

    } catch (Exception e) {
      Logger.error(
          "Runtime error updating subnet pool id: " + id + " error message: " + e.getMessage());
      return null;
    }

    return uuid;
  }

}
