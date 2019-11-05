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
 * @author Dario Valocchi(Ph.D.), UCL
 *
 * @author Guy Paz, Nokia
 *
 */

package sonata.kernel.vimadaptor.wrapper.openstack;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.LoggerFactory;

import sonata.kernel.vimadaptor.AdaptorCore;
import sonata.kernel.vimadaptor.commons.*;
import sonata.kernel.vimadaptor.commons.nsd.ConnectionPoint;
import sonata.kernel.vimadaptor.commons.nsd.ConnectionPointRecord;
import sonata.kernel.vimadaptor.commons.nsd.ConnectionPointType;
import sonata.kernel.vimadaptor.commons.nsd.InterfaceRecord;
import sonata.kernel.vimadaptor.commons.nsd.NetworkFunction;
import sonata.kernel.vimadaptor.commons.nsd.ServiceDescriptor;
import sonata.kernel.vimadaptor.commons.nsd.VirtualLink;
import sonata.kernel.vimadaptor.commons.vnfd.VirtualDeploymentUnit;
import sonata.kernel.vimadaptor.commons.vnfd.VnfDescriptor;
import sonata.kernel.vimadaptor.commons.vnfd.VnfVirtualLink;
import sonata.kernel.vimadaptor.wrapper.*;
import sonata.kernel.vimadaptor.wrapper.openstack.heat.HeatModel;
import sonata.kernel.vimadaptor.wrapper.openstack.heat.HeatPort;
import sonata.kernel.vimadaptor.wrapper.openstack.heat.HeatResource;
import sonata.kernel.vimadaptor.wrapper.openstack.heat.HeatServer;
import sonata.kernel.vimadaptor.wrapper.openstack.heat.HeatTemplate;
import sonata.kernel.vimadaptor.wrapper.openstack.heat.ServerPortsComposition;
import sonata.kernel.vimadaptor.wrapper.openstack.javastackclient.models.Image.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.Map.Entry;

import static com.sun.tools.doclint.Entity.or;

public class OpenStackHeatWrapper extends ComputeWrapper {

  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(OpenStackHeatWrapper.class);

  private String myPool;

  /**
   * Standard constructor for an Compute Wrapper of an OpenStack VIM using Heat.
   *
   * @param config the config object for this Compute Wrapper
   */
  public OpenStackHeatWrapper(WrapperConfiguration config) {
    super(config);
    String configuration = getConfig().getConfiguration();
    Logger.debug("Wrapper specific configuration: " + configuration);
    JSONTokener tokener = new JSONTokener(configuration);
    JSONObject object = (JSONObject) tokener.nextValue();
    // String tenant = object.getString("tenant");
    String tenantCidr = null;
    if (object.has("tenant_private_net_id")) {
      String tenantNetId = object.getString("tenant_private_net_id");
      int tenantNetLength = object.getInt("tenant_private_net_length");
      tenantCidr = tenantNetId + "/" + tenantNetLength;
    } else {
      tenantCidr = "10.0.0.0/8";
    }

    this.myPool = "tango-subnet-pool";
    // If vim not exist or cidr change
    if (!VimNetTable.getInstance().containsVim(this.getConfig().getUuid()) ||
        (VimNetTable.getInstance().containsVim(this.getConfig().getUuid()) &&
            !VimNetTable.getInstance().getCidr(this.getConfig().getUuid()).equals(tenantCidr))) {

      // create/update the pool
      if (this.createOrUpdateSubnetPools(myPool,tenantCidr,"27")) {
        VimNetTable.getInstance().registerVim(this.getConfig().getUuid(), tenantCidr);
      }
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see
   * sonata.kernel.vimadaptor.wrapper.ComputeWrapper#removeFunction(sonata.kernel.vimadaptor.commons
   * .FunctionRemovePayload, java.lang.String)
   */
  @Override
  public synchronized void removeFunction(FunctionRemovePayload data, String sid) {
    Long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    OpenStackHeatClient client = null;
    OpenStackNovaClient novaClient = null;

    try {
      client = new OpenStackHeatClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
      novaClient = new OpenStackNovaClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    } catch (IOException e) {
      Logger.error("OpenStackHeat wrapper - Unable to connect to the VIM");
      this.setChanged();
      WrapperStatusUpdate errorUpdate = new WrapperStatusUpdate(sid, "ERROR", e.getMessage());
      this.notifyObservers(errorUpdate);
      return;
    }

    Logger.debug(
        "Getting VIM stack name and UUID for service instance ID " + data.getServiceInstanceId());
    String stackUuid = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimUuid(data.getServiceInstanceId(), this.getConfig().getUuid());
    String stackName = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimName(data.getServiceInstanceId(), this.getConfig().getUuid());

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    mapper.setSerializationInclusion(Include.NON_NULL);

    HeatTemplate template = client.getStackTemplate(stackName, stackUuid);
    if (template == null) {
      Logger.error("Error retrieving the stack template.");
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Cannot retrieve service stack from VIM.");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    //locate resources that should be removed
    ArrayList<String> keysToRemove = new ArrayList<String>();
    for (Entry<String, Object> e: template.getResources().entrySet()) {
      if (e.getKey().contains(data.getVnfUuid())) {
        keysToRemove.add(e.getKey());
      }
    }
    //remove the resources
    for (String key: keysToRemove) {
      template.removeResource(key);
    }
    Logger.info("Updated stack for VNF removal created.");
    Logger.info("Serializing updated stack...");
    String stackString = null;
    try {
      stackString = mapper.writeValueAsString(template);
    } catch (JsonProcessingException e) {
      Logger.error(e.getMessage());
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Exception during VNF Deployment");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    Logger.debug(stackString);
    try {
      client.updateStack(stackName, stackUuid, stackString);
    } catch (Exception e) {
      Logger.error(e.getMessage());
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Exception during VNF Deployment");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    int counter = 0;
    int wait = 1000;
    int maxCounter = 50;
    int maxWait = 5000;
    String status = null;
    while ((status == null || !status.equals("UPDATE_COMPLETE") || !status.equals("UPDATE_FAILED"))
        && counter < maxCounter) {
      status = client.getStackStatus(stackName, stackUuid);
      Logger.info("Status of stack " + stackUuid + ": " + status);
      if (status != null && (status.equals("UPDATE_COMPLETE") || status.equals("UPDATE_FAILED"))) {
        break;
      }
      try {
        Thread.sleep(wait);
      } catch (InterruptedException e) {
        Logger.error(e.getMessage(), e);
      }
      counter++;
      wait = Math.min(wait * 2, maxWait);

    }

    if (status == null) {
      Logger.error("unable to contact the VIM to check the update status");
      WrapperStatusUpdate update = new WrapperStatusUpdate(sid, "ERROR",
          "Function deployment process failed. Can't get update status.");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    if (status.equals("UPDATE_FAILED")) {
      Logger.error("Heat Stack update process failed on the VIM side.");
      WrapperStatusUpdate update = new WrapperStatusUpdate(sid, "ERROR",
          "Function deployment process failed on the VIM side.");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }

    Logger.info("Creating function remove response");
    FunctionRemoveResponse response = new FunctionRemoveResponse();
    response.setRequestStatus("COMPLETED");
    response.setMessage("");

    String body = null;
    try {
      body = mapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      Logger.error(e.getMessage());
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Exception during VNF Deployment");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    Logger.info("Response created");
    Logger.info("body");

    WrapperBay.getInstance().getVimRepo().removeFunctionInstanceEntry(data.getVnfUuid(), this.getConfig().getUuid());
    WrapperStatusUpdate update = new WrapperStatusUpdate(sid, "SUCCESS", body);
    this.markAsChanged();
    this.notifyObservers(update);
    long stop = System.currentTimeMillis();

    Logger.info("[OpenStackWrapper]FunctionRemove-time: " + (stop - start) + " ms");

  }

  /*
   * (non-Javadoc)
   *
   * @see
   * sonata.kernel.vimadaptor.wrapper.ComputeWrapper#deployFunction(sonata.kernel.vimadaptor.commons
   * .FunctionDeployPayload, java.lang.String)
   */
  @Override
  public synchronized void deployFunction(FunctionDeployPayload data, String sid) {
    Long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    OpenStackHeatClient client = null;
    OpenStackNovaClient novaClient = null;
    OpenStackNeutronClient neutronClient = null;

    try {
      client = new OpenStackHeatClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
      novaClient = new OpenStackNovaClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
      neutronClient = new OpenStackNeutronClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    } catch (IOException e) {
      Logger.error("OpenStackHeat wrapper - Unable to connect to the VIM");
      this.setChanged();
      WrapperStatusUpdate errorUpdate = new WrapperStatusUpdate(sid, "ERROR", e.getMessage());
      this.notifyObservers(errorUpdate);
      return;
    }

    Logger.debug(
        "Getting VIM stack name and UUID for service instance ID " + data.getServiceInstanceId());
    String stackUuid = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimUuid(data.getServiceInstanceId(), this.getConfig().getUuid());
    String stackName = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimName(data.getServiceInstanceId(), this.getConfig().getUuid());

    ArrayList<Flavor> vimFlavors = novaClient.getFlavors();
    Collections.sort(vimFlavors);

    ArrayList<QosPolicy> vimPolicies = neutronClient.getPolicies();
    Collections.sort(vimPolicies);
    HeatModel stackAddition;

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    mapper.setSerializationInclusion(Include.NON_NULL);

    HeatTemplate template = client.getStackTemplate(stackName, stackUuid);
    if (template == null) {
      Logger.error("Error retrieving the stack template.");
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Cannot retrieve service stack from VIM.");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    try {
      stackAddition =
          translate(data.getVnfd(), vimFlavors, vimPolicies, template.getResources().keySet(), data.getServiceInstanceId(), data.getPublicKey());
    } catch (Exception e) {
      Logger.error("Error: " + e.getMessage());
      e.printStackTrace();
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Exception during VNFD translation.");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    for (HeatResource resource : stackAddition.getResources()) {
      template.putResource(resource.getResourceName(), resource);
    }

    Logger.info("Updated stack for VNF deployment created.");
    Logger.info("Serializing updated stack...");
    String stackString = null;
    try {
      stackString = mapper.writeValueAsString(template);
    } catch (JsonProcessingException e) {
      Logger.error(e.getMessage());
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Exception during VNF Deployment");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    Logger.debug(stackString);
    try {
      client.updateStack(stackName, stackUuid, stackString);
    } catch (Exception e) {
      Logger.error(e.getMessage());
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Exception during VNF Deployment");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    int counter = 0;
    int wait = 1000;
    int maxCounter = 50;
    int maxWait = 5000;
    String status = null;
    while ((status == null || !status.equals("UPDATE_COMPLETE") || !status.equals("UPDATE_FAILED"))
        && counter < maxCounter) {
      status = client.getStackStatus(stackName, stackUuid);
      Logger.info("Status of stack " + stackUuid + ": " + status);
      if (status != null && (status.equals("UPDATE_COMPLETE") || status.equals("UPDATE_FAILED"))) {
        break;
      }
      try {
        Thread.sleep(wait);
      } catch (InterruptedException e) {
        Logger.error(e.getMessage(), e);
      }
      counter++;
      wait = Math.min(wait * 2, maxWait);

    }

    if (status == null) {
      Logger.error("unable to contact the VIM to check the update status");
      WrapperStatusUpdate update = new WrapperStatusUpdate(sid, "ERROR",
          "Function deployment process failed. Can't get update status.");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    if (status.equals("UPDATE_FAILED")) {
      Logger.error("Heat Stack update process failed on the VIM side.");
      WrapperStatusUpdate update = new WrapperStatusUpdate(sid, "ERROR",
          "Function deployment process failed on the VIM side.");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }

    // counter = 0;
    // wait = 1000;
    // StackComposition composition = null;
    // while (composition == null && counter < maxCounter) {
    //   Logger.info("Getting composition of stack " + stackUuid);
    //   composition = client.getStackComposition(stackName, stackUuid);
    //   try {
    //     Thread.sleep(wait);
    //   } catch (InterruptedException e) {
    //     Logger.error(e.getMessage(), e);
    //   }
    //   counter++;
    //   wait = Math.min(wait * 2, maxWait);
    // }

    // if (composition == null) {
    //   Logger.error("unable to contact the VIM to get the stack composition");
    //   WrapperStatusUpdate update =
    //       new WrapperStatusUpdate(sid, "ERROR", "Unable to get updated stack composition");
    //   this.markAsChanged();
    //   this.notifyObservers(update);
    //   return;
    // }

    Logger.info("Creating function deploy response");
    // Aux data structures for efficient mapping
    Hashtable<String, VirtualDeploymentUnit> vduTable =
        new Hashtable<String, VirtualDeploymentUnit>();
    Hashtable<String, VduRecord> vdurTable = new Hashtable<String, VduRecord>();

    // Create the response

    FunctionDeployResponse response = new FunctionDeployResponse();
    VnfDescriptor vnfd = data.getVnfd();
    response.setRequestStatus("COMPLETED");
    response.setInstanceVimUuid(stackUuid);
    response.setInstanceName(stackName);
    response.setVimUuid(this.getConfig().getUuid());
    response.setMessage("");


    VnfRecord vnfr = new VnfRecord();
    vnfr.setDescriptorVersion("vnfr-schema-01");
    vnfr.setId(vnfd.getInstanceUuid());
    vnfr.setDescriptorReference(vnfd.getUuid());
    vnfr.setStatus(Status.offline);
    // vnfr.setDescriptorReferenceName(vnf.getName());
    // vnfr.setDescriptorReferenceVendor(vnf.getVendor());
    // vnfr.setDescriptorReferenceVersion(vnf.getVersion());

    for (VirtualDeploymentUnit vdu : vnfd.getVirtualDeploymentUnits()) {
      Logger.debug("Inspecting VDU " + vdu.getId());
      VduRecord vdur = new VduRecord();
      vdur.setId(vdu.getId());
      vdur.setNumberOfInstances(1);
      vdur.setVduReference(vnfd.getName() + ":" + vdu.getId());
      vdur.setVmImage(vdu.getVmImage());
      vdurTable.put(vdur.getVduReference(), vdur);
      vnfr.addVdu(vdur);
      Logger.debug("VDU table created: " + vduTable.toString());

      HeatServer server = client.getServerComposition(stackName, stackUuid, vdu.getId());

      String[] identifiers = server.getServerName().split("\\.");
      String vnfName = identifiers[0];
      String vduName = identifiers[1];
      // String instanceId = identifiers[2];
      String vnfcIndex = identifiers[3];
      if (vdu.getId().equals(vduName)) {
        VnfcInstance vnfc = new VnfcInstance();
        vnfc.setId(vnfcIndex);
        vnfc.setVimId(data.getVimUuid());
        vnfc.setVcId(server.getServerId());
        vnfc.setHostId(server.getHostId());
        ArrayList<ConnectionPointRecord> cpRecords = new ArrayList<ConnectionPointRecord>();
        ServerPortsComposition ports = client.getServerPortsComposition(stackName, stackUuid, vdu.getId());
        for (ConnectionPoint cp : vdu.getConnectionPoints()) {
          Logger.debug("Mapping CP " + cp.getId());
          Logger.debug("Looking for port " + vnfd.getName() + "." + vdu.getId() + "." + cp.getId()
              + "." + vnfd.getInstanceUuid());
          ConnectionPointRecord cpr = new ConnectionPointRecord();
          cpr.setId(cp.getId());


          // add each composition.ports information in the response. The IP, the netmask (and
          // maybe MAC address)
          boolean found = false;
          for (HeatPort port : ports.getPorts()) {
            Logger.debug("port " + port.getPortName());
            if (port.getPortName().equals(vnfd.getName() + "." + vdu.getId() + "." + cp.getId()
                + "." + vnfd.getInstanceUuid())) {
              found = true;
              Logger.debug("Found! Filling VDUR parameters");
              InterfaceRecord ip = new InterfaceRecord();
              if (port.getFloatinIp() != null) {
                ip.setAddress(port.getFloatinIp());
                ip.setHardwareAddress(port.getMacAddress());
                IpMapping ipMap = new IpMapping();
                ipMap.setFloatingIp(port.getFloatinIp());
                ipMap.setInternalIp(port.getIpAddress());
                response.addIp(ipMap);
                // Logger.info("Port:" + port.getPortName() + "- Addr: " +
                // port.getFloatingIp());
              } else {
                ip.setAddress(port.getIpAddress());
                ip.setHardwareAddress(port.getMacAddress());
                // Logger.info("Port:" + port.getPortName() + "- Addr: " +
                // port.getFloatingIp());
                ip.setNetmask("255.255.255.248");

              }
              cpr.setInterface(ip);
              cpr.setType(cp.getType());
              break;
            }
          }
          if (!found) {
            Logger.error("Can't find the VIM port that maps to this CP");
          }
          cpRecords.add(cpr);
        }
        vnfc.setConnectionPoints(cpRecords);
        VduRecord referenceVdur = vdurTable.get(vnfd.getName() + ":" + vdu.getId());
        referenceVdur.addVnfcInstance(vnfc);

      }

    }

    response.setVnfr(vnfr);
    String body = null;
    try {
      body = mapper.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      Logger.error(e.getMessage());
      WrapperStatusUpdate update =
          new WrapperStatusUpdate(sid, "ERROR", "Exception during VNF Deployment");
      this.markAsChanged();
      this.notifyObservers(update);
      return;
    }
    Logger.info("Response created");
    // Logger.info("body");

    WrapperBay.getInstance().getVimRepo().writeFunctionInstanceEntry(vnfd.getInstanceUuid(),
        data.getServiceInstanceId(), this.getConfig().getUuid());
    WrapperStatusUpdate update = new WrapperStatusUpdate(sid, "SUCCESS", body);
    this.markAsChanged();
    this.notifyObservers(update);
    long stop = System.currentTimeMillis();

    Logger.info("[OpenStackWrapper]FunctionDeploy-time: " + (stop - start) + " ms");
  }

  @Override
  @Deprecated
  public boolean deployService(ServiceDeployPayload data, String callSid) {

    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT
    OpenStackHeatClient client = null;
    OpenStackNovaClient novaClient = null;

    try {
      client = new OpenStackHeatClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
      novaClient = new OpenStackNovaClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    } catch (IOException e) {
      Logger.error("OpenStackHeat wrapper - Unable to connect to the VIM");
      this.setChanged();
      WrapperStatusUpdate errorUpdate = new WrapperStatusUpdate(callSid, "ERROR", e.getMessage());
      this.notifyObservers(errorUpdate);
      return false;
    }
    ArrayList<Flavor> vimFlavors = novaClient.getFlavors();
    Collections.sort(vimFlavors);
    HeatModel stack;
    try {
      stack = translate(data, vimFlavors);

      HeatTemplate template = new HeatTemplate();
      for (HeatResource resource : stack.getResources()) {
        template.putResource(resource.getResourceName(), resource);
      }
      DeployServiceFsm fsm = new DeployServiceFsm(this, client, callSid, data, template);

      Thread thread = new Thread(fsm);
      thread.start();
    } catch (Exception e) {
      this.setChanged();
      WrapperStatusUpdate errorUpdate = new WrapperStatusUpdate(callSid, "ERROR", e.getMessage());
      this.notifyObservers(errorUpdate);
      return false;
    }

    return true;

  }

  /**
   * Returns a heat template translated from the given descriptors. Mainly used for unit testing
   * scope
   *
   * @param data the service descriptors to translate
   * @param vimFlavors the list of available compute flavors
   * @return an HeatTemplate object translated from the given descriptors
   * @throws Exception if unable to translate the descriptor.
   */
  public HeatTemplate getHeatTemplateFromSonataDescriptor(ServiceDeployPayload data,
                                                          ArrayList<Flavor> vimFlavors) throws Exception {
    HeatModel model = this.translate(data, vimFlavors);
    HeatTemplate template = new HeatTemplate();
    for (HeatResource resource : model.getResources()) {
      template.putResource(resource.getResourceName(), resource);
    }
    return template;
  }

  @Override
  public ResourceUtilisation getResourceUtilisation() {
    long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    ResourceUtilisation output = null;
    Logger.info("OpenStack wrapper - Getting resource utilisation...");
    OpenStackNovaClient client;
    try {
      client = new OpenStackNovaClient(getConfig().getVimEndpoint(), getConfig().getAuthUserName(),
          getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
      output = client.getResourceUtilizasion();
      Logger.info("OpenStack wrapper - Resource utilisation retrieved.");
    } catch (IOException e) {
      Logger.error("OpenStack wrapper - Unable to connect to PoP.");;
      output = null;
    }
    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]getResourceUtilisation-time: " + (stop - start) + " ms");
    return output;
  }



  /*
   * (non-Javadoc)
   *
   * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#isImageStored(java.lang.String)
   */
  @Override
  public boolean isImageStored(VnfImage image, String callSid) {
    long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    Logger.debug("Checking image: " + image.getUuid());
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // END COMMENT


    OpenStackGlanceClient glance = null;
    try {
      glance = new OpenStackGlanceClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    } catch (IOException e) {
      Logger.error("OpenStackHeat wrapper - Unable to connect to the VIM");
      this.setChanged();
      WrapperStatusUpdate errorUpdate = new WrapperStatusUpdate(callSid, "ERROR", e.getMessage());
      this.notifyObservers(errorUpdate);
      return false;
    }
    ArrayList<Image> glanceImages = glance.listImages();
    boolean out = false;
    if (image.getChecksum() == null) {
      out = searchImageByName(image.getUuid(), glanceImages);
    } else {
      out = searchImageByChecksum(image.getChecksum(), glanceImages);
    }
    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]isImageStored-time: " + (stop - start) + " ms");
    return out;
  }

  /*
   * (non-Javadoc)
   *
   * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#prepareService(java.lang.String,
   * ArrayList<VirtualLink> virtualLinks)
   */
  @Deprecated
  public boolean prepareService(String instanceId, ArrayList<VirtualLink> virtualLinks) throws Exception {
    long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    // To prepare a service instance management and data networks/subnets must be created.
    OpenStackHeatClient client = new OpenStackHeatClient(getConfig().getVimEndpoint().toString(),
        getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    OpenStackNeutronClient neutronClient = new OpenStackNeutronClient(getConfig().getVimEndpoint().toString(),
        getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);

    ArrayList<QosPolicy> vimPolicies = neutronClient.getPolicies();
    Collections.sort(vimPolicies);

    HeatTemplate template = createInitStackTemplate(instanceId, virtualLinks, vimPolicies);

    Logger.info("Deploying new stack for service preparation.");
    ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
    Logger.info("Serializing stack...");
    try {
      String stackString = mapper.writeValueAsString(template);
      Logger.debug(stackString);
      String stackName = "SonataService-" + instanceId;
      Logger.info("Pushing stack to Heat...");
      String stackUuid = client.createStack(stackName, stackString);

      if (stackUuid == null) {
        Logger.error("unable to contact the VIM to instantiate the service");
        return false;
      }
      int counter = 0;
      int wait = 1000;
      int maxWait = 15000;
      int maxCounter = 50;
      String status = null;
      while ((status == null || !status.equals("CREATE_COMPLETE")
          || !status.equals("CREATE_FAILED")) && counter < maxCounter) {
        status = client.getStackStatus(stackName, stackUuid);
        Logger.info("Status of stack " + stackUuid + ": " + status);
        if (status != null
            && (status.equals("CREATE_COMPLETE") || status.equals("CREATE_FAILED"))) {
          break;
        }
        try {
          Thread.sleep(wait);
        } catch (InterruptedException e) {
          Logger.error(e.getMessage(), e);
        }
        counter++;
        wait = Math.min(wait * 2, maxWait);
      }

      if (status == null) {
        Logger.error("unable to contact the VIM to check the instantiation status");
        return false;
      }
      if (status.equals("CREATE_FAILED")) {
        Logger.error("Heat Stack creation process failed on the VIM side.");
        return false;
      }
      Logger.info("VIM prepared successfully. Creating record in Infra Repo.");
      WrapperBay.getInstance().getVimRepo().writeServiceInstanceEntry(instanceId, stackUuid,
          stackName, this.getConfig().getUuid());

    } catch (Exception e) {
      Logger.error("Error during stack creation.");
      Logger.error(e.getMessage());
      return false;
    }
    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]PrepareService-time: " + (stop - start) + " ms");
    return true;

  }


  /*
   * (non-Javadoc)
   *
   * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#networkCreate(java.lang.String,
   * ArrayList<VirtualLink> virtualLinks)
   */
  @Override
  public boolean networkCreate(String instanceId, ArrayList<VirtualLink> virtualLinks) throws Exception {
    Long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    OpenStackHeatClient client = null;
    OpenStackNeutronClient neutronClient = null;

    try {
      client = new OpenStackHeatClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
      neutronClient = new OpenStackNeutronClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    } catch (IOException e) {
      Logger.error("OpenStackHeat wrapper - Unable to connect to the VIM");
      return false;
    }

    ArrayList<QosPolicy> vimPolicies = neutronClient.getPolicies();
    Collections.sort(vimPolicies);
    HeatModel stackAddition;

    ObjectMapper mapper_y = new ObjectMapper(new YAMLFactory());
    mapper_y.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    mapper_y.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper_y.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    mapper_y.setSerializationInclusion(Include.NON_NULL);

    try {
      stackAddition =  translateNetwork(instanceId, virtualLinks, vimPolicies);
    } catch (Exception e) {
      Logger.error("Error: " + e.getMessage());
      e.printStackTrace();
      return false;
    }

    String stackUuid = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimUuid(instanceId, this.getConfig().getUuid());
    String stackName = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimName(instanceId, this.getConfig().getUuid());

    if (stackUuid != null && stackName != null) {
      Logger.info("Update the stack.");

      HeatTemplate template = client.getStackTemplate(stackName, stackUuid);
      if (template == null) {
        Logger.error("Error retrieving the stack template.");
        return false;
      }
      for (HeatResource resource : stackAddition.getResources()) {
        template.putResource(resource.getResourceName(), resource);
      }

      Logger.info("Updated stack for VNF network create created.");
      Logger.info("Serializing updated stack...");
      String stackString = null;
      try {
        stackString = mapper_y.writeValueAsString(template);
      } catch (JsonProcessingException e) {
        Logger.error(e.getMessage());
        return false;
      }
      Logger.debug(stackString);
      try {
        client.updateStack(stackName, stackUuid, stackString);
      } catch (Exception e) {
        Logger.error(e.getMessage());
        return false;
      }

      int counter = 0;
      int wait = 1000;
      int maxCounter = 50;
      int maxWait = 5000;
      String status = null;
      while ((status == null || !status.equals("UPDATE_COMPLETE") || !status.equals("UPDATE_FAILED"))
          && counter < maxCounter) {
        status = client.getStackStatus(stackName, stackUuid);
        Logger.info("Status of stack " + stackUuid + ": " + status);
        if (status != null && (status.equals("UPDATE_COMPLETE") || status.equals("UPDATE_FAILED"))) {
          break;
        }
        try {
          Thread.sleep(wait);
        } catch (InterruptedException e) {
          Logger.error(e.getMessage(), e);
        }
        counter++;
        wait = Math.min(wait * 2, maxWait);

      }

      if (status == null) {
        Logger.error("unable to contact the VIM to check the update status");
        return false;
      }
      if (status.equals("UPDATE_FAILED")) {
        Logger.error("Heat Stack update process failed on the VIM side.");
        return false;
      }

      Logger.info("VIM updated successfully.");

    } else {
      Logger.info("Deploying new stack.");
      ObjectMapper mapper = SonataManifestMapper.getSonataMapper();

      HeatTemplate template = new HeatTemplate();
      for (HeatResource resource : stackAddition.getResources()) {
        template.putResource(resource.getResourceName(), resource);
      }

      Logger.info("Serializing stack...");
      try {
        String stackString = mapper.writeValueAsString(template);
        Logger.debug(stackString);
        stackName = "SonataService-" + instanceId;
        Logger.info("Pushing stack to Heat...");
        stackUuid = client.createStack(stackName, stackString);

        if (stackUuid == null) {
          Logger.error("unable to contact the VIM to instantiate the service");
          return false;
        }
        int counter = 0;
        int wait = 1000;
        int maxWait = 15000;
        int maxCounter = 50;
        String status = null;
        while ((status == null || !status.equals("CREATE_COMPLETE")
            || !status.equals("CREATE_FAILED")) && counter < maxCounter) {
          status = client.getStackStatus(stackName, stackUuid);
          Logger.info("Status of stack " + stackUuid + ": " + status);
          if (status != null
              && (status.equals("CREATE_COMPLETE") || status.equals("CREATE_FAILED"))) {
            break;
          }
          try {
            Thread.sleep(wait);
          } catch (InterruptedException e) {
            Logger.error(e.getMessage(), e);
          }
          counter++;
          wait = Math.min(wait * 2, maxWait);
        }

        if (status == null) {
          Logger.error("unable to contact the VIM to check the instantiation status");
          return false;
        }
        if (status.equals("CREATE_FAILED")) {
          Logger.error("Heat Stack creation process failed on the VIM side.");
          return false;
        }
        Logger.info("VIM prepared successfully. Creating record in Infra Repo.");
        WrapperBay.getInstance().getVimRepo().writeServiceInstanceEntry(instanceId, stackUuid,
            stackName, this.getConfig().getUuid());

      } catch (Exception e) {
        Logger.error("Error during stack creation.");
        Logger.error(e.getMessage());
        return false;
      }
    }

    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]NetworkCreate-time: " + (stop - start) + " ms");
    return true;
  }


  /*
   * (non-Javadoc)
   *
   * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#networkDelete(java.lang.String,
   * ArrayList<VirtualLink> virtualLinks)
   */
  @Override
  public boolean networkDelete(String instanceId, ArrayList<VirtualLink> virtualLinks) throws Exception {
    Long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    OpenStackHeatClient client = null;

    try {
      client = new OpenStackHeatClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    } catch (IOException e) {
      Logger.error("OpenStackHeat wrapper - Unable to connect to the VIM");
      return false;
    }

    Logger.debug("Getting VIM stack name and UUID for service instance ID " + instanceId);
    String stackUuid = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimUuid(instanceId, this.getConfig().getUuid());
    String stackName = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimName(instanceId, this.getConfig().getUuid());

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS);
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    mapper.setSerializationInclusion(Include.NON_NULL);


    HeatTemplate template = client.getStackTemplate(stackName, stackUuid);
    if (template == null) {
      Logger.error("Error retrieving the stack template.");
      return false;
    }

    //locate resources that should be removed
    ArrayList<String> keysToRemove = new ArrayList<String>();
    for (Entry<String, Object> e: template.getResources().entrySet()) {
      for (VirtualLink link : virtualLinks) {
        if (e.getKey().contains(link.getId())) {
          keysToRemove.add(e.getKey());
          break;
        }
      }
    }
    //remove the resources
    for (String key: keysToRemove) {
      template.removeResource(key);
    }

    if (!template.getResources().isEmpty()) {

      Logger.info("Updated stack for VNF network delete created.");
      Logger.info("Serializing updated stack...");
      String stackString = null;
      try {
        stackString = mapper.writeValueAsString(template);
      } catch (JsonProcessingException e) {
        Logger.error(e.getMessage());
        return false;
      }
      Logger.debug(stackString);
      try {
        client.updateStack(stackName, stackUuid, stackString);
      } catch (Exception e) {
        Logger.error(e.getMessage());
        return false;
      }

      int counter = 0;
      int wait = 1000;
      int maxCounter = 50;
      int maxWait = 5000;
      String status = null;
      while ((status == null || !status.equals("UPDATE_COMPLETE") || !status.equals("UPDATE_FAILED"))
          && counter < maxCounter) {
        status = client.getStackStatus(stackName, stackUuid);
        Logger.info("Status of stack " + stackUuid + ": " + status);
        if (status != null && (status.equals("UPDATE_COMPLETE") || status.equals("UPDATE_FAILED"))) {
          break;
        }
        try {
          Thread.sleep(wait);
        } catch (InterruptedException e) {
          Logger.error(e.getMessage(), e);
        }
        counter++;
        wait = Math.min(wait * 2, maxWait);
      }

      if (status == null) {
        Logger.error("unable to contact the VIM to check the update status");
        return false;
      }
      if (status.equals("UPDATE_FAILED")) {
        Logger.error("Heat Stack update process failed on the VIM side.");
        return false;
      }

      Logger.info("VIM updated successfully.");

    } else {

      try {
        String output = client.deleteStack(stackName, stackUuid);

        if (output.equals("DELETED")) {
          int counter = 0;
          int wait = 1000;
          int maxCounter = 20;
          int maxWait = 5000;
          String status = null;
          while (counter < maxCounter) {
            status = client.getStackStatus(stackName, stackUuid);
            Logger.info("Status of stack " + stackUuid + ": " + status);
            if (status == null || (status.equals("DELETE_COMPLETE") || status.equals("DELETE_FAILED"))) {
              break;
            }
            try {
              Thread.sleep(wait);
            } catch (InterruptedException e) {
              Logger.error(e.getMessage(), e);
            }
            counter++;
            wait = Math.min(wait * 2, maxWait);

          }

          if (status != null && status.equals("DELETE_FAILED")) {
            Logger.error("Heat Stack delete process failed on the VIM side.");
            return false;
          }

          WrapperBay.getInstance().getVimRepo().removeServiceInstanceEntry(instanceId, this.getConfig().getUuid());
        }
      } catch (Exception e) {
        Logger.error(e.getMessage());
        return false;
      }
      Logger.info("Stack deleted successfully.");
    }

    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]NetworkDelete-time: " + (stop - start) + " ms");
    return true;
  }

  /*
   * (non-Javadoc)
   *
   * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#removeImage(java.lang.String)
   */
  @Override
  public void removeImage(VnfImage image) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   *
   * @see
   * sonata.kernel.vimadaptor.wrapper.ComputeWrapper#removeService(sonata.kernel.vimadaptor.commons
   * .ServiceRemovePayload, java.lang.String)
   */
  @Override
  public void removeService(ServiceRemovePayload data, String callSid) {
    long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT
    VimRepo repo = WrapperBay.getInstance().getVimRepo();
    Logger.info("Trying to remove NS instance: " + data.getServiceInstanceId());
    String stackName = repo.getServiceInstanceVimName(data.getServiceInstanceId(), this.getConfig().getUuid());
    String stackUuid = repo.getServiceInstanceVimUuid(data.getServiceInstanceId(), this.getConfig().getUuid());
    Logger.info("NS instance mapped to stack name: " + stackName);
    Logger.info("NS instance mapped to stack uuid: " + stackUuid);

    OpenStackHeatClient client = null;

    try {
      client = new OpenStackHeatClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    } catch (IOException e) {
      Logger.error("OpenStackHeat wrapper - Unable to connect to the VIM");
      this.setChanged();
      WrapperStatusUpdate errorUpdate = new WrapperStatusUpdate(callSid, "ERROR", e.getMessage());
      this.notifyObservers(errorUpdate);
    }

    try {
      String output = client.deleteStack(stackName, stackUuid);

      if (output.equals("DELETED")) {
        int counter = 0;
        int wait = 1000;
        int maxCounter = 20;
        int maxWait = 5000;
        String status = null;
        while (counter < maxCounter) {
          status = client.getStackStatus(stackName, stackUuid);
          Logger.info("Status of stack " + stackUuid + ": " + status);
          if (status == null || (status.equals("DELETE_COMPLETE") || status.equals("DELETE_FAILED"))) {
            break;
          }
          try {
            Thread.sleep(wait);
          } catch (InterruptedException e) {
            Logger.error(e.getMessage(), e);
          }
          counter++;
          wait = Math.min(wait * 2, maxWait);

        }

        if (status != null && status.equals("DELETE_FAILED")) {
          Logger.error("Heat Stack delete process failed on the VIM side.");
          WrapperStatusUpdate errorUpdate = new WrapperStatusUpdate(callSid, "ERROR",
              "Remove service process failed on the VIM side.");
          this.setChanged();
          this.notifyObservers(errorUpdate);
          return;
        }

        repo.removeServiceInstanceEntry(data.getServiceInstanceId(), this.getConfig().getUuid());
        this.setChanged();
        String body =
            "{\"status\":\"COMPLETED\",\"wrapper_uuid\":\"" + this.getConfig().getUuid() + "\"}";
        WrapperStatusUpdate update = new WrapperStatusUpdate(callSid, "SUCCESS", body);
        this.notifyObservers(update);
      }
    } catch (Exception e) {
      e.printStackTrace();
      this.setChanged();
      WrapperStatusUpdate errorUpdate = new WrapperStatusUpdate(callSid, "ERROR", e.getMessage());
      this.notifyObservers(errorUpdate);
    }
    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]RemoveService-time: " + (stop - start) + " ms");
  }

  @Override
  public void scaleFunction(FunctionScalePayload data, String sid) {

    String stackUuid = WrapperBay.getInstance().getVimRepo()
        .getServiceInstanceVimUuid(data.getServiceInstanceId(), this.getConfig().getUuid());

    Logger.info("Scaling stack");
    // TODO - smendel - need to get the number of required instances from each vdu
    // TODO - smendel - get execution result, if needed use polling - see deployFunction

    Logger.info("Creating function scale response");

    // TODO - smendel - create IA response to FLM - see deployFunction
  }

  /*
   * (non-Javadoc)
   *
   * @see sonata.kernel.vimadaptor.wrapper.ComputeWrapper#uploadImage(java.lang.String)
   */
  @Override
  public void uploadImage(VnfImage image) throws IOException {
    long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // END COMMENT

    OpenStackGlanceClient glance =
        new OpenStackGlanceClient(getConfig().getVimEndpoint().toString(),
            getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);

    Logger.debug("Creating new image: " + image.getUuid());
    String imageUuid = glance.createImage(image.getUuid());

    URL website = new URL(image.getUrl());
    String fileName = website.getPath().substring(website.getPath().lastIndexOf("/"));
    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
    String fileAbsolutePath = "/tmp/" + fileName;
    FileOutputStream fos = new FileOutputStream(fileAbsolutePath);
    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    fos.flush();
    fos.close();

    Logger.debug("Uploading new image from " + fileAbsolutePath);

    glance.uploadImage(imageUuid, fileAbsolutePath);


    File f = new File(fileAbsolutePath);
    if (f.delete()) {
      Logger.debug("temporary image file deleted successfully from local environment.");
    } else {
      Logger.error("Error deleting the temporary image file " + fileName
          + " from local environment. Relevant VNF: " + image.getUuid());
      throw new IOException("Error deleting the temporary image file " + fileName
          + " from local environment. Relevant VNF: " + image.getUuid());
    }
    long stop = System.currentTimeMillis();

    Logger.info("[OpenStackWrapper]UploadImage-time: " + (stop - start) + " ms");
  }

  @Override
  public ArrayList<ExtNetwork> getNetworks() {

    long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    // String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    ArrayList<ExtNetwork> output = null;
    Logger.info("OpenStack wrapper - Getting networks ...");
    try {
      OpenStackNeutronClient neutronClient = new OpenStackNeutronClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);

      output = neutronClient.getNetworks();

      Logger.info("OpenStack wrapper - Networks retrieved.");
    } catch (IOException e) {
      Logger.error("OpenStack wrapper - Unable to connect to PoP.");;
      output = null;
    }
    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]getNetworks-time: " + (stop - start) + " ms");
    return output;
  }

  @Override
  public ArrayList<Router> getRouters() {

    long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    String tenantExtNet = object.getString("tenant_ext_net");
    // String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    ArrayList<Router> output = null;
    Logger.info("OpenStack wrapper - Getting routers ...");
    try {
      OpenStackNeutronClient neutronClient = new OpenStackNeutronClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);

      output = neutronClient.getRouters(tenantExtNet);

      Logger.info("OpenStack wrapper - Routers retrieved.");
    } catch (IOException e) {
      Logger.error("OpenStack wrapper - Unable to connect to PoP.");;
      output = null;
    }
    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]getRouters-time: " + (stop - start) + " ms");
    return output;
  }

  public Boolean createOrUpdateSubnetPools(String name, String prefix, String defaultPrefixlen) {

    long start = System.currentTimeMillis();
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }

    ArrayList<String> prefixes = new ArrayList<>();
    String id = null;

    boolean output = false;
    OpenStackNeutronClient neutronClient = null;
    try {
      neutronClient = new OpenStackNeutronClient(getConfig().getVimEndpoint().toString(),
          getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    } catch (IOException e) {
      Logger.error("OpenStack wrapper - Unable to connect to PoP.");;
      long stop = System.currentTimeMillis();
      Logger.info("[OpenStackWrapper]createOrUpdateSubnetPools-time: " + (stop - start) + " ms");
      return output;
    }
    Logger.info("OpenStack wrapper - List Subnet Pools ...");
    ArrayList<SubnetPool> subnetPools = null;
    try {
      subnetPools = neutronClient.getSubnetPools();

      Logger.info("OpenStack wrapper - Subnet Pools Listed.");
    } catch (Exception e) {
      Logger.error("OpenStack wrapper - Unable to list subnet. ERROR:"+e.getMessage());;
      output = false;
    }

    if (subnetPools != null) {
      for (SubnetPool inputSubnetPool : subnetPools) {
        if (inputSubnetPool.getName().equals(name)) {
          id = inputSubnetPool.getId();
          prefixes = inputSubnetPool.getPrefixes();
          break;
        }
      }
    }

    if (id == null) {
      // create
      prefixes.add(prefix);
      Logger.info("OpenStack wrapper - Creating Subnet Pool ...");
      try {
        String uuid = neutronClient.createSubnetPool(name, prefixes, defaultPrefixlen);
        if (uuid != null) {
          output = true;
          Logger.info("OpenStack wrapper - Subnet Pool Created.");
        }
      } catch (Exception e) {
        Logger.error("OpenStack wrapper - Unable to create subnet. ERROR:"+e.getMessage());;
        output = false;
      }

    } else {
      // update if prefix not exist
      if (!prefixes.contains(prefix)) {
        prefixes.add(prefix);
        Logger.info("OpenStack wrapper - Updating Subnet Pool ...");
        try {
          String uuid = neutronClient.updateSubnetPool(id, prefixes);
          if (uuid != null) {
            output = true;
            Logger.info("OpenStack wrapper - Subnet Pool Updated.");
          }
        } catch (Exception e) {
          Logger.error("OpenStack wrapper - Unable to update subnet. ERROR:"+e.getMessage());;
          output = false;
        }
      } else {
        // already exist with this prefix
        output = true;
      }
    }

    long stop = System.currentTimeMillis();
    Logger.info("[OpenStackWrapper]createOrUpdateSubnetPools-time: " + (stop - start) + " ms");
    return output;
  }

  @Deprecated
  private HeatTemplate createInitStackTemplate(String instanceId, ArrayList<VirtualLink> virtualLinks, ArrayList<QosPolicy> policies) throws Exception {

    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(this.getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    // String tenant = object.getString("tenant");
    // String tenantExtNet = object.getString("tenant_ext_net");
    String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    Logger.debug("Creating init stack template");

    HeatModel model = new HeatModel();

    for (VirtualLink link : virtualLinks) {
      if (link.getNetworkId() != null) {
        continue;
      }
      HeatResource network = new HeatResource();
      network.setType("OS::Neutron::Net");
      network.setName(link.getId());
      network.putProperty("name", link.getId());

      String qosPolicy = null;
      if (link.getQos() != null) {
        if (!searchQosPolicyByName(link.getQos(),policies)) {
          Logger.error("Cannot find the Qos Policy: " + link.getQos());
          throw new Exception("Cannot find the Qos Policy: " + link.getQos());
        }
        qosPolicy = link.getQos();
      } else if (link.getQosRequirements()!= null) {

        double bandwidthLimitInMbps = 0;
        double minimumBandwidthInMbps = 0;
        if (link.getQosRequirements().getBandwidthLimit()!= null) {
          bandwidthLimitInMbps = link.getQosRequirements().getBandwidthLimit().getBandwidth()
              * link.getQosRequirements().getBandwidthLimit().getBandwidthUnit().getMultiplier();
        }

        if (link.getQosRequirements().getMinimumBandwidth()!= null) {
          minimumBandwidthInMbps = link.getQosRequirements().getMinimumBandwidth().getBandwidth()
              * link.getQosRequirements().getMinimumBandwidth().getBandwidthUnit().getMultiplier();
        }

        try {
          qosPolicy = this.selectQosPolicy(bandwidthLimitInMbps, minimumBandwidthInMbps, policies);
        } catch (Exception e) {
          Logger.error("Exception while searching for available  Qos Policies for the requirements: "
              + e.getMessage());
          throw new Exception("Cannot find an available  Qos Policies for requirements. Bandwidth Limit: "
              + bandwidthLimitInMbps + " - Minimum Bandwidth: " + minimumBandwidthInMbps);
        }
      }
      if (qosPolicy != null) {
        // add the qos to the port
        network.putProperty("qos_policy", qosPolicy);
      }

      model.addResource(network);
      HeatResource subnet = new HeatResource();
      subnet.setType("OS::Neutron::Subnet");
      subnet.setName("subnet." + link.getId());
      subnet.putProperty("name", "subnet." + link.getId());
      if (link.getCidr() != null) {
        subnet.putProperty("cidr", link.getCidr());
      } else {
        subnet.putProperty("subnetpool", myPool);
      }
      if (link.isDhcp() != null) {
        subnet.putProperty("enable_dhcp", link.isDhcp());
      }
      String[] dnsArray = {"8.8.8.8"};
      subnet.putProperty("dns_nameservers", dnsArray);
      HashMap<String, Object> netMap = new HashMap<String, Object>();
      netMap.put("get_resource", link.getId());
      subnet.putProperty("network", netMap);
      model.addResource(subnet);

      if ((link.isAccess() == null) || link.isAccess()) {
        // internal router interface for network
        HeatResource routerInterface = new HeatResource();
        routerInterface.setType("OS::Neutron::RouterInterface");
        routerInterface.setName("routerInterface." + link.getId());
        HashMap<String, Object> subnetMapInt = new HashMap<String, Object>();
        subnetMapInt.put("get_resource", "subnet." + link.getId());
        routerInterface.putProperty("subnet", subnetMapInt);
        routerInterface.putProperty("router", tenantExtRouter);
        model.addResource(routerInterface);
      }

    }

    model.prepare();

    HeatTemplate template = new HeatTemplate();
    Logger.debug("Created " + model.getResources().size() + " resources.");
    for (HeatResource resource : model.getResources()) {
      template.putResource(resource.getResourceName(), resource);
    }
    return template;
  }

  /**
   * @param vmImageMd5 the checksum of the image to search;
   * @throws IOException if the VIM cannot be contacted to retrieve the list of available images;
   */
  private String getImageIdByImageChecksum(String vmImageMd5) throws IOException {
    String imageId = null;
    Logger.debug("Searching Image Checksum: " + vmImageMd5);
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    String tenant = object.getString("tenant");
    String identityPort = null;
    if (object.has("identity_port")) {
      identityPort = object.getString("identity_port");
    }
    OpenStackGlanceClient glance = null;
    glance = new OpenStackGlanceClient(getConfig().getVimEndpoint().toString(),
        getConfig().getAuthUserName(), getConfig().getAuthPass(), getConfig().getDomain(), tenant, identityPort);
    ArrayList<Image> glanceImages = glance.listImages();
    for (Image image : glanceImages) {
      if (image != null && image.getChecksum() != null
          && (image.getChecksum().equals(vmImageMd5))) {
        imageId = image.getId();
        break;
      }
    }
    return imageId;
  }

  private String getTenant() {
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    return object.getString("tenant");
  }



  private boolean searchImageByChecksum(String imageChecksum, ArrayList<Image> glanceImages) {
    Logger.debug("Image lookup based on image checksum...");
    for (Image glanceImage : glanceImages) {
      if (glanceImage.getName() == null) continue;
      Logger.debug("Checking " + glanceImage.getName());
      if (glanceImage.getChecksum() == null) continue;
      if (glanceImage.getChecksum().equals(imageChecksum)) {
        return true;
      }
    }
    return false;
  }

  private boolean searchImageByName(String imageName, ArrayList<Image> glanceImages) {
    Logger.debug("Image lookup based on image name...");
    for (Image glanceImage : glanceImages) {
      if (glanceImage.getName() == null) continue;
      Logger.debug("Checking " + glanceImage.getName());
      if (glanceImage.getName().equals(imageName)) {
        return true;
      }
    }
    return false;
  }

  private String selectFlavor(int vcpu, double memoryInGB, double storageIngGB,
                              ArrayList<Flavor> vimFlavors) {
    Logger.debug("Flavor Selecting routine. Resource req: " + vcpu + " cpus - " + memoryInGB
        + " GB mem - " + storageIngGB + " GB sto");
    for (Flavor flavor : vimFlavors) {
      if (vcpu <= flavor.getVcpu() && ((memoryInGB * 1024) <= flavor.getRam())
          && (storageIngGB <= flavor.getStorage())) {
        Logger.debug("Flavor found:" + flavor.getFlavorName());
        return flavor.getFlavorName();
      }
    }
    Logger.debug("Flavor not found");
    return null;
  }

  private boolean searchFlavorByName(String flavorName, ArrayList<Flavor> vimFlavors) {
    Logger.debug("Flavor lookup based on flavor name...");
    for (Flavor flavor : vimFlavors) {
      if (flavor.getFlavorName() == null) continue;
      Logger.debug("Checking " + flavor.getFlavorName());
      if (flavor.getFlavorName().equals(flavorName)) {
        return true;
      }
    }
    return false;
  }

  private String selectQosPolicy(double bandwidthLimitInMbps, double minimumBandwidthInMbps,
                                 ArrayList<QosPolicy> vimQosPolicies) {
    Logger.debug("Qos Policy Selecting routine. Resource req: " + bandwidthLimitInMbps
        + " Mbps max limit - " + minimumBandwidthInMbps + " Mbps guaranteed");
    for (QosPolicy policy : vimQosPolicies) {
      if (((bandwidthLimitInMbps * 1000) <=
          policy.getQosRulesByTypeAndDirection("bandwidth_limit","egress"))
          && ((minimumBandwidthInMbps * 1000) <=
          policy.getQosRulesByTypeAndDirection("minimum_bandwidth","egress"))) {
        Logger.debug("Qos Policy found:" + policy.getQosPolicyName());
        return policy.getQosPolicyName();
      }
    }
    Logger.debug("Flavor not found");
    return null;
  }

  private boolean searchQosPolicyByName(String qosPolicyName, ArrayList<QosPolicy> vimQosPolicies) {
    Logger.debug("Qos Policy lookup based on Qos Policy name...");
    for (QosPolicy policy : vimQosPolicies) {
      if (policy.getQosPolicyName() == null) continue;
      Logger.debug("Checking " + policy.getQosPolicyName());
      if (policy.getQosPolicyName().equals(qosPolicyName)) {
        return true;
      }
    }
    return false;
  }

  @Deprecated
  private HeatModel translate(ServiceDeployPayload data, ArrayList<Flavor> vimFlavors)
      throws Exception {

    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    // String tenant = object.getString("tenant");
    String tenantExtNet = object.getString("tenant_ext_net");
    String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT


    ServiceDescriptor nsd = data.getNsd();

    // Allocate Ip Addresses on the basis of the service requirements:
    int numberOfSubnets = 1;
    int subnetIndex = 0;

    for (VnfDescriptor vnfd : data.getVnfdList()) {
      ArrayList<VnfVirtualLink> links = vnfd.getVirtualLinks();
      for (VnfVirtualLink link : links) {
        if (!link.getId().equals("mgmt")) {
          numberOfSubnets++;
        }
      }
    }

    // Create the management Net and subnet for all the VNFCs and VNFs
    HeatResource mgmtNetwork = new HeatResource();
    mgmtNetwork.setType("OS::Neutron::Net");
    mgmtNetwork.setName(nsd.getName() + ".mgmt.net." + nsd.getInstanceUuid());
    mgmtNetwork.putProperty("name", nsd.getName() + ".mgmt.net." + nsd.getInstanceUuid());



    HeatModel model = new HeatModel();
    model.addResource(mgmtNetwork);

    HeatResource mgmtSubnet = new HeatResource();

    mgmtSubnet.setType("OS::Neutron::Subnet");
    mgmtSubnet.setName(nsd.getName() + ".mgmt.subnet." + nsd.getInstanceUuid());
    mgmtSubnet.putProperty("name", nsd.getName() + ".mgmt.subnet." + nsd.getInstanceUuid());
    mgmtSubnet.putProperty("subnetpool", myPool);

    // mgmtSubnet.putProperty("cidr", "192.168." + subnetIndex + ".0/24");
    // mgmtSubnet.putProperty("gateway_ip", "192.168." + subnetIndex + ".1");

    subnetIndex++;
    HashMap<String, Object> mgmtNetMap = new HashMap<String, Object>();
    mgmtNetMap.put("get_resource", nsd.getName() + ".mgmt.net." + nsd.getInstanceUuid());
    mgmtSubnet.putProperty("network", mgmtNetMap);
    model.addResource(mgmtSubnet);


    // Internal mgmt router interface
    HeatResource mgmtRouterInterface = new HeatResource();
    mgmtRouterInterface.setType("OS::Neutron::RouterInterface");
    mgmtRouterInterface.setName(nsd.getName() + ".mgmt.internal." + nsd.getInstanceUuid());
    HashMap<String, Object> mgmtSubnetMapInt = new HashMap<String, Object>();
    mgmtSubnetMapInt.put("get_resource", nsd.getName() + ".mgmt.subnet." + nsd.getInstanceUuid());
    mgmtRouterInterface.putProperty("subnet", mgmtSubnetMapInt);
    mgmtRouterInterface.putProperty("router", tenantExtRouter);
    model.addResource(mgmtRouterInterface);

    // One virtual router for NSD virtual links connecting VNFS (no router for external virtual
    // links and management links)

    ArrayList<VnfDescriptor> vnfs = data.getVnfdList();
    for (VirtualLink link : nsd.getVirtualLinks()) {
      ArrayList<String> connectionPointReference = link.getConnectionPointsReference();
      boolean isInterVnf = true;
      boolean isMgmt = link.getId().equals("mgmt");
      for (String cpRef : connectionPointReference) {
        if (cpRef.startsWith("ns:")) {
          isInterVnf = false;
          break;
        }
      }
      if (isInterVnf && !isMgmt) {
        HeatResource router = new HeatResource();
        router.setName(nsd.getName() + "." + link.getId() + "." + nsd.getInstanceUuid());
        router.setType("OS::Neutron::Router");
        router.putProperty("name",
            nsd.getName() + "." + link.getId() + "." + nsd.getInstanceUuid());
        model.addResource(router);
      }
    }

    ArrayList<String> mgmtPortNames = new ArrayList<String>();

    for (VnfDescriptor vnfd : vnfs) {
      // One network and subnet for vnf virtual link (mgmt links handled later)
      ArrayList<VnfVirtualLink> links = vnfd.getVirtualLinks();
      for (VnfVirtualLink link : links) {
        if (!link.getId().equals("mgmt")) {
          HeatResource network = new HeatResource();
          network.setType("OS::Neutron::Net");
          network.setName(vnfd.getName() + "." + link.getId() + ".net." + nsd.getInstanceUuid());
          network.putProperty("name",
              vnfd.getName() + "." + link.getId() + ".net." + nsd.getInstanceUuid());
          model.addResource(network);
          HeatResource subnet = new HeatResource();
          subnet.setType("OS::Neutron::Subnet");
          subnet.setName(vnfd.getName() + "." + link.getId() + ".subnet." + nsd.getInstanceUuid());
          subnet.putProperty("name",
              vnfd.getName() + "." + link.getId() + ".subnet." + nsd.getInstanceUuid());
          subnet.putProperty("subnetpool", myPool);
          // getConfig() parameter
          // String[] dnsArray = { "10.30.0.11", "8.8.8.8" };
          // TODO DNS should not be hardcoded, VMs should automatically get OpenStack subnet DNS.
          String[] dnsArray = {"8.8.8.8"};
          subnet.putProperty("dns_nameservers", dnsArray);
          // subnet.putProperty("gateway_ip", myPool.getGateway(cidr));
          // subnet.putProperty("cidr", "192.168." + subnetIndex + ".0/24");
          // subnet.putProperty("gateway_ip", "192.168." + subnetIndex + ".1");
          subnetIndex++;
          HashMap<String, Object> netMap = new HashMap<String, Object>();
          netMap.put("get_resource",
              vnfd.getName() + "." + link.getId() + ".net." + nsd.getInstanceUuid());
          subnet.putProperty("network", netMap);
          model.addResource(subnet);
        }
      }
      // One virtual machine for each VDU

      for (VirtualDeploymentUnit vdu : vnfd.getVirtualDeploymentUnits()) {
        HeatResource server = new HeatResource();
        server.setType("OS::Nova::Server");
        server.setName(vnfd.getName() + "." + vdu.getId() + "." + nsd.getInstanceUuid());
        server.putProperty("name",
            vnfd.getName() + "." + vdu.getId() + "." + nsd.getInstanceUuid());
        server.putProperty("image", vdu.getVmImage());
        String flavorName = null;
        if (vdu.getVmFlavor() != null) {
          flavorName = vdu.getVmFlavor();
          if (!searchFlavorByName(flavorName,vimFlavors)) {
            Logger.error("Cannot find the flavor: " + flavorName);
            throw new Exception("Cannot find the flavor: " + flavorName);
          }
        } else {
          int vcpu = vdu.getResourceRequirements().getCpu().getVcpus();
          double memoryInBytes = vdu.getResourceRequirements().getMemory().getSize()
              * vdu.getResourceRequirements().getMemory().getSizeUnit().getMultiplier();
          double storageInBytes = vdu.getResourceRequirements().getStorage().getSize()
              * vdu.getResourceRequirements().getStorage().getSizeUnit().getMultiplier();
          try {
            flavorName = this.selectFlavor(vcpu, memoryInBytes, storageInBytes, vimFlavors);
          } catch (Exception e) {
            Logger.error("Exception while searching for available flavor for the requirements: "
                + e.getMessage());
            throw new Exception("Cannot find an available flavor for requirements. CPU: " + vcpu
                + " - mem: " + memoryInBytes + " - sto: " + storageInBytes);
          }
          if (flavorName == null) {
            Logger.error("Cannot find an available flavor for the requirements. CPU: " + vcpu
                + " - mem: " + memoryInBytes + " - sto: " + storageInBytes);
            throw new Exception("Cannot find an available flavor for requirements. CPU: " + vcpu
                + " - mem: " + memoryInBytes + " - sto: " + storageInBytes);
          }
        }
        server.putProperty("flavor", flavorName);
        ArrayList<HashMap<String, Object>> net = new ArrayList<HashMap<String, Object>>();
        for (ConnectionPoint cp : vdu.getConnectionPoints()) {
          // create the port resource
          boolean isMgmtPort = false;
          String linkIdReference = null;
          for (VnfVirtualLink link : vnfd.getVirtualLinks()) {
            if (link.getConnectionPointsReference().contains(cp.getId())) {
              if (link.getId().equals("mgmt")) {
                isMgmtPort = true;
              } else {
                linkIdReference = link.getId();
              }
              break;
            }
          }
          if (isMgmtPort) {
            // connect this VNFC CP to the mgmt network
            HeatResource port = new HeatResource();
            port.setType("OS::Neutron::Port");
            port.setName(vnfd.getName() + "." + cp.getId() + "." + nsd.getInstanceUuid());
            port.putProperty("name",
                vnfd.getName() + "." + cp.getId() + "." + nsd.getInstanceUuid());
            HashMap<String, Object> netMap = new HashMap<String, Object>();
            netMap.put("get_resource", nsd.getName() + ".mgmt.net." + nsd.getInstanceUuid());
            port.putProperty("network", netMap);
            if (cp.getMac() != null) {
              port.putProperty("mac_address", cp.getMac());
            }
            if (cp.getIp() != null) {
              ArrayList<HashMap<String, Object>> ip = new ArrayList<HashMap<String, Object>>();
              // add the fixed ip to the port
              HashMap<String, Object> n1 = new HashMap<String, Object>();
              n1.put("ip_address", cp.getIp());
              ip.add(n1);

              port.putProperty("fixed_ips", ip);
            }
            if (cp.getQos() != null) {
              // add the qos to the port
              port.putProperty("qos_policy", cp.getQos());
            }
            if (cp.getSecurityGroups() != null) {
              // add the security groups to the port
              port.putProperty("security_groups", cp.getSecurityGroups());
            }
            model.addResource(port);
            mgmtPortNames.add(vnfd.getName() + "." + cp.getId() + "." + nsd.getInstanceUuid());

            // add the port to the server
            HashMap<String, Object> n1 = new HashMap<String, Object>();
            HashMap<String, Object> portMap = new HashMap<String, Object>();
            portMap.put("get_resource",
                vnfd.getName() + "." + cp.getId() + "." + nsd.getInstanceUuid());
            n1.put("port", portMap);
            net.add(n1);
          } else if (linkIdReference != null) {
            HeatResource port = new HeatResource();
            port.setType("OS::Neutron::Port");
            port.setName(vnfd.getName() + "." + cp.getId() + "." + nsd.getInstanceUuid());
            port.putProperty("name",
                vnfd.getName() + "." + cp.getId() + "." + nsd.getInstanceUuid());
            HashMap<String, Object> netMap = new HashMap<String, Object>();
            netMap.put("get_resource",
                vnfd.getName() + "." + linkIdReference + ".net." + nsd.getInstanceUuid());
            port.putProperty("network", netMap);
            if (cp.getMac() != null) {
              port.putProperty("mac_address", cp.getMac());
            }
            if (cp.getIp() != null) {
              ArrayList<HashMap<String, Object>> ip = new ArrayList<HashMap<String, Object>>();
              // add the fixed ip to the port
              HashMap<String, Object> n1 = new HashMap<String, Object>();
              n1.put("ip_address", cp.getIp());
              ip.add(n1);

              port.putProperty("fixed_ips", ip);
            }
            if (cp.getQos() != null) {
              // add the qos to the port
              port.putProperty("qos_policy", cp.getQos());
            }
            if (cp.getSecurityGroups() != null) {
              // add the security groups to the port
              port.putProperty("security_groups", cp.getSecurityGroups());
            }
            model.addResource(port);
            // add the port to the server
            HashMap<String, Object> n1 = new HashMap<String, Object>();
            HashMap<String, Object> portMap = new HashMap<String, Object>();
            portMap.put("get_resource",
                vnfd.getName() + "." + cp.getId() + "." + nsd.getInstanceUuid());
            n1.put("port", portMap);
            net.add(n1);
          }
        }
        server.putProperty("networks", net);
        model.addResource(server);
      }

      // One Router interface per VNF cp connected to a inter-VNF link of the NSD
      for (ConnectionPoint cp : vnfd.getConnectionPoints()) {
        boolean isMgmtPort = cp.getId().contains("mgmt");

        if (!isMgmtPort) {
          // Resolve vnf_id from vnf_name
          String vnfId = null;
          // Logger.info("[TRANSLATION] VNFD.name: " + vnfd.getName());

          for (NetworkFunction vnf : nsd.getNetworkFunctions()) {
            // Logger.info("[TRANSLATION] NSD.network_functions.vnf_name: " + vnf.getVnfName());
            // Logger.info("[TRANSLATION] NSD.network_functions.vnf_id: " + vnf.getVnfId());

            if (vnf.getVnfName().equals(vnfd.getName())) {
              vnfId = vnf.getVnfId();
            }
          }

          if (vnfId == null) {
            throw new Exception("Error binding VNFD.connection_point: "
                + "Cannot resolve VNFD.name in NSD.network_functions. " + "VNFD.name = "
                + vnfd.getName() + " - VFND.connection_point = " + cp.getId());

          }
          boolean isInOut = false;
          String nsVirtualLink = null;
          boolean isVirtualLinkFound = false;
          for (VirtualLink link : nsd.getVirtualLinks()) {
            if (link.getConnectionPointsReference().contains(cp.getId().replace("vnf", vnfId))) {
              isVirtualLinkFound = true;
              for (String cpRef : link.getConnectionPointsReference()) {
                if (cpRef.startsWith("ns:")) {
                  isInOut = true;
                  break;
                }
              }
              if (!isInOut) {
                nsVirtualLink = nsd.getName() + "." + link.getId() + "." + nsd.getInstanceUuid();
              }
              break;
            }
          }
          if (!isVirtualLinkFound) {
            throw new Exception("Error binding VNFD.connection_point:"
                + " Cannot find NSD.virtual_link attached to VNFD.connection_point."
                + " VNFD.connection_point = " + vnfd.getName() + "." + cp.getId());
          }
          if (!isInOut) {
            HeatResource routerInterface = new HeatResource();
            routerInterface.setType("OS::Neutron::RouterInterface");
            routerInterface
                .setName(vnfd.getName() + "." + cp.getId() + "." + nsd.getInstanceUuid());
            for (VnfVirtualLink link : links) {
              if (link.getConnectionPointsReference().contains(cp.getId())) {
                HashMap<String, Object> subnetMap = new HashMap<String, Object>();
                subnetMap.put("get_resource",
                    vnfd.getName() + "." + link.getId() + ".subnet." + nsd.getInstanceUuid());
                routerInterface.putProperty("subnet", subnetMap);
                break;
              }
            }

            // Attach to the virtual router
            HashMap<String, Object> routerMap = new HashMap<String, Object>();
            routerMap.put("get_resource", nsVirtualLink);
            routerInterface.putProperty("router", routerMap);
            model.addResource(routerInterface);
          }
        }
      }

    }

    for (String portName : mgmtPortNames) {
      // allocate floating IP
      HeatResource floatingIp = new HeatResource();
      floatingIp.setType("OS::Neutron::FloatingIP");
      floatingIp.setName("floating:" + portName);

      floatingIp.putProperty("floating_network_id", tenantExtNet);

      HashMap<String, Object> floatMapPort = new HashMap<String, Object>();
      floatMapPort.put("get_resource", portName);
      floatingIp.putProperty("port_id", floatMapPort);

      model.addResource(floatingIp);
    }
    model.prepare();
    return model;
  }

  private HeatModel translate(VnfDescriptor vnfd, ArrayList<Flavor> flavors, ArrayList<QosPolicy> policies, Set<String> resources, String serviceInstanceUuid,
                              String publicKey) throws Exception {
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    // String tenant = object.getString("tenant");
    String tenantExtNet = object.getString("tenant_ext_net");
    String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT
    HeatModel model = new HeatModel();
    ArrayList<String> publicPortNames = new ArrayList<String>();

    ArrayList<HashMap<String,Object>> configList = new ArrayList<HashMap<String, Object>>();

    ArrayList<VnfVirtualLink> NewVnfVirtualLinks = new ArrayList<VnfVirtualLink>();

    boolean hasPubKey = (publicKey != null);

    if (hasPubKey) {
      HeatResource keypair = new HeatResource();
      keypair.setType("OS::Nova::KeyPair");
      keypair.setName(vnfd.getName() + "_" + vnfd.getInstanceUuid() + "_keypair");
      keypair.putProperty("name", vnfd.getName() + "_" + vnfd.getInstanceUuid() + "_keypair");
      keypair.putProperty("save_private_key", "false");
      keypair.putProperty("public_key", publicKey);
      model.addResource(keypair);

      HashMap<String, Object> userMap = new HashMap<String, Object>();
      userMap.put("name", "sonatamano");
      userMap.put("gecos", "SONATA MANO admin user");
      String[] userGroups = {"adm", "audio", "cdrom", "dialout", "dip", "floppy", "netdev",
          "plugdev", "sudo", "video"};
      userMap.put("groups", userGroups);
      userMap.put("shell", "/bin/bash");
      String[] keys = {publicKey};
      userMap.put("ssh-authorized-keys", keys);
      userMap.put("home", "/home/sonatamano");
      Object[] usersList = {"default", userMap};

      HashMap<String, Object> keyCloudConfigMap = new HashMap<String, Object>();
      keyCloudConfigMap.put("users", usersList);

      HeatResource keycloudConfigObject = new HeatResource();
      keycloudConfigObject.setType("OS::Heat::CloudConfig");
      keycloudConfigObject.setName(vnfd.getName() + "_" + vnfd.getInstanceUuid() + "_keyCloudConfig");
      keycloudConfigObject.putProperty("cloud_config", keyCloudConfigMap);
      model.addResource(keycloudConfigObject);

      HashMap<String, Object> keyInitMap = new HashMap<String, Object>();
      keyInitMap.put("get_resource", vnfd.getName() + "_" + vnfd.getInstanceUuid() + "_keyCloudConfig");

      HashMap<String,Object> partMap1 = new HashMap<String, Object>();
      partMap1.put("config", keyInitMap);

      configList.add(partMap1);
    }

    // addSpAddressCloudConfigObject(vnfd, instanceUuid, model);

    // HashMap<String, Object> spAddressInitMap = new HashMap<String, Object>();
    // spAddressInitMap.put("get_resource", vnfd.getName() + "_" + instanceUuid + "_spAddressCloudConfig");

    // HashMap<String,Object> partMap2 = new HashMap<String, Object>();
    // partMap2.put("config", spAddressInitMap);

    // configList.add(partMap2);

    for (VirtualDeploymentUnit vdu : vnfd.getVirtualDeploymentUnits()) {
      Logger.debug("Each VDU goes into a resource group with a number of Heat Server...");
      HeatResource resourceGroup = new HeatResource();
      resourceGroup.setType("OS::Heat::ResourceGroup");
      resourceGroup.setName(vnfd.getName() + "." + vdu.getId() + "." + vnfd.getInstanceUuid());
      resourceGroup.putProperty("count", new Integer(1));
      String image =
          vnfd.getVendor() + "_" + vnfd.getName() + "_" + vnfd.getVersion() + "_" + vdu.getId();
      if (vdu.getVmImageMd5() != null) {
        image = getImageIdByImageChecksum(vdu.getVmImageMd5());
      }

      Logger.debug("image selected:" + image);
      HeatResource server = new HeatResource();
      server.setType("OS::Nova::Server");
      server.setName(null);
      server.putProperty("name",
          vnfd.getName() + "." + vdu.getId() + "." + vnfd.getInstanceUuid() + ".instance%index%");
      server.putProperty("image", image);

      String userData = vdu.getUserData();
      Logger.debug("User data for this vdu:" + userData);

      boolean vduHasUserData = (vdu.getUserData() != null);
      ArrayList<HashMap<String,Object>> newConfigList = new ArrayList<HashMap<String, Object>>(configList);

      if (vduHasUserData) {
        Logger.debug("Adding cloud-init resource");
        server.putProperty("config_drive", true);
        if (configList.isEmpty()){
          server.putProperty("user_data", userData);
          server.putProperty("user_data_format", "RAW");

        } else {
          HeatResource userDataObject = new HeatResource();
          userDataObject.setType("OS::Heat::SoftwareConfig");
          userDataObject.setName(vdu.getId() + "_" + vnfd.getInstanceUuid() + "_cloudInitConfig");
          userDataObject.putProperty("group", "ungrouped");
          userDataObject.putProperty("config", vdu.getUserData());
          model.addResource(userDataObject);

          HashMap<String, Object> cloudInitMap = new HashMap<String, Object>();
          cloudInitMap.put("get_resource", vdu.getId() + "_" + vnfd.getInstanceUuid() + "_cloudInitConfig");

          HashMap<String,Object> partMap3 = new HashMap<String, Object>();
          partMap3.put("config", cloudInitMap);
          newConfigList.add(partMap3);
        }
      }

      for (HashMap config : configList){
        Logger.debug(config.toString());
      }
      if (!newConfigList.isEmpty()){
        HeatResource serverInitObject = new HeatResource();
        serverInitObject.setType("OS::Heat::MultipartMime");
        serverInitObject.setName(vdu.getId() + "_" + vnfd.getInstanceUuid() + "_serverInit");
        serverInitObject.putProperty("parts", newConfigList);
        model.addResource(serverInitObject);

        HashMap<String, Object> userDataMap = new HashMap<String, Object>();
        userDataMap.put("get_resource", vdu.getId() + "_" + vnfd.getInstanceUuid() + "_serverInit");
        server.putProperty("user_data", userDataMap);
        server.putProperty("user_data_format", "SOFTWARE_CONFIG");
      }
      String flavorName = null;
      if (vdu.getVmFlavor() != null) {
        flavorName = vdu.getVmFlavor();
        if (!searchFlavorByName(flavorName,flavors)) {
          Logger.error("Cannot find the flavor: " + flavorName);
          throw new Exception("Cannot find the flavor: " + flavorName);
        }
      } else {
        int vcpu = vdu.getResourceRequirements().getCpu().getVcpus();
        double memoryInGB = vdu.getResourceRequirements().getMemory().getSize()
            * vdu.getResourceRequirements().getMemory().getSizeUnit().getMultiplier();
        double storageInGB = vdu.getResourceRequirements().getStorage().getSize()
            * vdu.getResourceRequirements().getStorage().getSizeUnit().getMultiplier();

        try {
          flavorName = this.selectFlavor(vcpu, memoryInGB, storageInGB, flavors);
        } catch (Exception e) {
          Logger.error("Exception while searching for available flavor for the requirements: "
              + e.getMessage());
          throw new Exception("Cannot find an available flavor for requirements. CPU: " + vcpu
              + " - mem: " + memoryInGB + " - sto: " + storageInGB);
        }
        if (flavorName == null) {
          Logger.error("Cannot find an available flavor for the requirements. CPU: " + vcpu
              + " - mem: " + memoryInGB + " - sto: " + storageInGB);
          throw new Exception("Cannot find an available flavor for requirements. CPU: " + vcpu
              + " - mem: " + memoryInGB + " - sto: " + storageInGB);
        }
      }
      server.putProperty("flavor", flavorName);
      ArrayList<HashMap<String, Object>> net = new ArrayList<HashMap<String, Object>>();
      for (ConnectionPoint vduCp : vdu.getConnectionPoints()) {
        // create the port resource
        HeatResource port = new HeatResource();
        port.setType("OS::Neutron::Port");
        String cpQualifiedName =
            vnfd.getName() + "." + vdu.getId() + "." + vduCp.getId() + "." + vnfd.getInstanceUuid();
        port.setName(cpQualifiedName);
        port.putProperty("name", cpQualifiedName);
        HashMap<String, Object> netMap = new HashMap<String, Object>();
        Logger.debug("Mapping CP Type to the relevant network");

        String netId = null;
        // Already exist network
        if (vduCp.getNetworkId() != null) {
          netId = vduCp.getNetworkId();

          if ((vduCp.getFIp() != null ) && vduCp.getFIp()) {
            publicPortNames.add(cpQualifiedName);
          }
        } else {
          //Need to create or use the vnf network
          VnfVirtualLink link = null;
          for (VnfVirtualLink vnfLink : vnfd.getVirtualLinks()) {
            if (vnfLink.getConnectionPointsReference().contains(vdu.getId() + ":" + vduCp.getId())) {
              link = vnfLink;
              if (link.getNetworkId() != null) {
                netId = link.getNetworkId();
              }
              break;
            }
          }

          if (link == null) {
            Logger.error("Cannot find the virtual link for  connection point: " + vduCp.getId() + " from vdu: " + vdu.getId());
            throw new Exception("Cannot find the virtual link for  connection point: " + vduCp.getId() + " from vdu: " + vdu.getId());
          }
          // Already exist network
          if (netId != null) {
            if ((vduCp.getFIp() != null ) && vduCp.getFIp()) {
              publicPortNames.add(cpQualifiedName);
            }

            // Already created the vnf virtual link
          } else if (NewVnfVirtualLinks.contains(link)) {
            netMap.put("get_resource", "SonataService." + link.getId() + ".net." + vnfd.getInstanceUuid());
            if (vduCp.getType() != ConnectionPointType.INT ) {
              //Check if the network was external access (access)
              if ((link.isAccess() == null) || link.isAccess()) {
                publicPortNames.add(cpQualifiedName);
              }
            }
            // Need to create the vnf virtual link
          } else {
            HeatResource network = new HeatResource();
            network.setType("OS::Neutron::Net");
            network.setName("SonataService." + link.getId() + ".net." + vnfd.getInstanceUuid());
            network.putProperty("name",
                "SonataService." + link.getId() + ".net." + vnfd.getInstanceUuid());

            String qosPolicy = null;
            if (link.getQos() != null) {
              if (!searchQosPolicyByName(link.getQos(),policies)) {
                Logger.error("Cannot find the Qos Policy: " + link.getQos());
                throw new Exception("Cannot find the Qos Policy: " + link.getQos());
              }
              qosPolicy = link.getQos();
            } else if (link.getQosRequirements()!= null) {

              double bandwidthLimitInMbps = 0;
              double minimumBandwidthInMbps = 0;
              if (link.getQosRequirements().getBandwidthLimit()!= null) {
                bandwidthLimitInMbps = link.getQosRequirements().getBandwidthLimit().getBandwidth()
                    * link.getQosRequirements().getBandwidthLimit().getBandwidthUnit().getMultiplier();
              }

              if (link.getQosRequirements().getMinimumBandwidth()!= null) {
                minimumBandwidthInMbps = link.getQosRequirements().getMinimumBandwidth().getBandwidth()
                    * link.getQosRequirements().getMinimumBandwidth().getBandwidthUnit().getMultiplier();
              }

              try {
                qosPolicy = this.selectQosPolicy(bandwidthLimitInMbps, minimumBandwidthInMbps, policies);
              } catch (Exception e) {
                Logger.error("Exception while searching for available  Qos Policies for the requirements: "
                    + e.getMessage());
                throw new Exception("Cannot find an available  Qos Policies for requirements. Bandwidth Limit: "
                    + bandwidthLimitInMbps + " - Minimum Bandwidth: " + minimumBandwidthInMbps);
              }
            }
            if (qosPolicy != null) {
              // add the qos to the port
              network.putProperty("qos_policy", qosPolicy);
            }

            model.addResource(network);
            HeatResource subnet = new HeatResource();
            subnet.setType("OS::Neutron::Subnet");
            subnet.setName("SonataService." + link.getId() + ".subnet." + vnfd.getInstanceUuid());
            subnet.putProperty("name",
                "SonataService." + link.getId() + ".subnet." + vnfd.getInstanceUuid());
            if (link.getCidr() != null) {
              subnet.putProperty("cidr", link.getCidr());
            } else {
              subnet.putProperty("subnetpool", myPool);
            }
            if (link.isDhcp() != null) {
              subnet.putProperty("enable_dhcp", link.isDhcp());
            }
            String[] dnsArray = {"8.8.8.8"};
            subnet.putProperty("dns_nameservers", dnsArray);

            HashMap<String, Object> subnetMap = new HashMap<String, Object>();
            subnetMap.put("get_resource",
                "SonataService." + link.getId() + ".net." + vnfd.getInstanceUuid());
            subnet.putProperty("network", subnetMap);
            model.addResource(subnet);

            if ((link.isAccess() == null) || link.isAccess()) {
              // internal router interface for network
              HeatResource routerInterface = new HeatResource();
              routerInterface.setType("OS::Neutron::RouterInterface");
              routerInterface.setName("SonataService." + link.getId() + ".internal." + vnfd.getInstanceUuid());
              HashMap<String, Object> subnetMapInt = new HashMap<String, Object>();
              subnetMapInt.put("get_resource", "SonataService." + link.getId() + ".subnet." + vnfd.getInstanceUuid());
              routerInterface.putProperty("subnet", subnetMapInt);
              routerInterface.putProperty("router", tenantExtRouter);
              model.addResource(routerInterface);
            }

            NewVnfVirtualLinks.add(link);

            netMap.put("get_resource", "SonataService." + link.getId() + ".net." + vnfd.getInstanceUuid());
            if ((vduCp.getFIp() != null ) && vduCp.getFIp()) {
              publicPortNames.add(cpQualifiedName);
            }

          }

        }
        if (netId != null) {
          if (resources.contains(netId)) {
            netMap.put("get_resource", netId);
            port.putProperty("network", netMap);
          } else {
            port.putProperty("network", netId);
          }
          if (netId.equals(tenantExtNet)) {
            publicPortNames.remove(cpQualifiedName);
          }
        } else {
          port.putProperty("network", netMap);
        }
        if (vduCp.getMac() != null) {
          port.putProperty("mac_address", vduCp.getMac());
        }
        if (vduCp.getIp() != null) {
          ArrayList<HashMap<String, Object>> ip = new ArrayList<HashMap<String, Object>>();
          // add the fixed ip to the port
          HashMap<String, Object> n1 = new HashMap<String, Object>();
          n1.put("ip_address", vduCp.getIp());
          ip.add(n1);

          port.putProperty("fixed_ips", ip);
        }
        String qosPolicy = null;
        if (vduCp.getQos() != null) {
          if (!searchQosPolicyByName(vduCp.getQos(),policies)) {
            Logger.error("Cannot find the Qos Policy: " + vduCp.getQos());
            throw new Exception("Cannot find the Qos Policy: " + vduCp.getQos());
          }
          qosPolicy = vduCp.getQos();
        } else if (vduCp.getQosRequirements()!= null) {

          double bandwidthLimitInMbps = 0;
          double minimumBandwidthInMbps = 0;
          if (vduCp.getQosRequirements().getBandwidthLimit()!= null) {
            bandwidthLimitInMbps = vduCp.getQosRequirements().getBandwidthLimit().getBandwidth()
                * vduCp.getQosRequirements().getBandwidthLimit().getBandwidthUnit().getMultiplier();
          }

          if (vduCp.getQosRequirements().getMinimumBandwidth()!= null) {
            minimumBandwidthInMbps = vduCp.getQosRequirements().getMinimumBandwidth().getBandwidth()
                * vduCp.getQosRequirements().getMinimumBandwidth().getBandwidthUnit().getMultiplier();
          }

          try {
            qosPolicy = this.selectQosPolicy(bandwidthLimitInMbps, minimumBandwidthInMbps, policies);
          } catch (Exception e) {
            Logger.error("Exception while searching for available  Qos Policies for the requirements: "
                + e.getMessage());
            throw new Exception("Cannot find an available  Qos Policies for requirements. Bandwidth Limit: "
                + bandwidthLimitInMbps + " - Minimum Bandwidth: " + minimumBandwidthInMbps);
          }
        }
        if (qosPolicy != null) {
          // add the qos to the port
          port.putProperty("qos_policy", qosPolicy);
        }
        if (vduCp.getSecurityGroups() != null) {
          if (vduCp.getSecurityGroups().isEmpty()) {
            // disable port security
            port.putProperty("port_security_enabled", "False");
          } else {
            // add the security groups to the port
            port.putProperty("security_groups", vduCp.getSecurityGroups());
          }
        }
        model.addResource(port);

        // add the port to the server
        HashMap<String, Object> n1 = new HashMap<String, Object>();
        HashMap<String, Object> portMap = new HashMap<String, Object>();
        portMap.put("get_resource", cpQualifiedName);
        n1.put("port", portMap);
        net.add(n1);
      }
      server.putProperty("networks", net);
      resourceGroup.putProperty("resource_def", server);
      model.addResource(resourceGroup);
    }

    for (String portName : publicPortNames) {
      // allocate floating IP
      HeatResource floatingIp = new HeatResource();
      floatingIp.setType("OS::Neutron::FloatingIP");
      floatingIp.setName("floating." + portName);


      floatingIp.putProperty("floating_network_id", tenantExtNet);

      HashMap<String, Object> floatMapPort = new HashMap<String, Object>();
      floatMapPort.put("get_resource", portName);
      floatingIp.putProperty("port_id", floatMapPort);
      model.addResource(floatingIp);
    }
    model.prepare();
    return model;
  }

  private HeatModel translateNetwork(String instanceId, ArrayList<VirtualLink> virtualLinks, ArrayList<QosPolicy> policies) throws Exception {
    // TODO This values should be per User, now they are per VIM. This should be re-designed once
    // user management is in place.
    JSONTokener tokener = new JSONTokener(this.getConfig().getConfiguration());
    JSONObject object = (JSONObject) tokener.nextValue();
    // String tenant = object.getString("tenant");
    // String tenantExtNet = object.getString("tenant_ext_net");
    String tenantExtRouter = object.getString("tenant_ext_router");
    // END COMMENT

    HeatModel model = new HeatModel();

    for (VirtualLink link : virtualLinks) {
      if (link.getNetworkId() != null) {
        continue;
      }
      HeatResource network = new HeatResource();
      network.setType("OS::Neutron::Net");
      network.setName(link.getId());
      network.putProperty("name", link.getId());

      String qosPolicy = null;
      if (link.getQos() != null) {
        if (!searchQosPolicyByName(link.getQos(),policies)) {
          Logger.error("Cannot find the Qos Policy: " + link.getQos());
          throw new Exception("Cannot find the Qos Policy: " + link.getQos());
        }
        qosPolicy = link.getQos();
      } else if (link.getQosRequirements()!= null) {

        double bandwidthLimitInMbps = 0;
        double minimumBandwidthInMbps = 0;
        if (link.getQosRequirements().getBandwidthLimit()!= null) {
          bandwidthLimitInMbps = link.getQosRequirements().getBandwidthLimit().getBandwidth()
              * link.getQosRequirements().getBandwidthLimit().getBandwidthUnit().getMultiplier();
        }

        if (link.getQosRequirements().getMinimumBandwidth()!= null) {
          minimumBandwidthInMbps = link.getQosRequirements().getMinimumBandwidth().getBandwidth()
              * link.getQosRequirements().getMinimumBandwidth().getBandwidthUnit().getMultiplier();
        }

        try {
          qosPolicy = this.selectQosPolicy(bandwidthLimitInMbps, minimumBandwidthInMbps, policies);
        } catch (Exception e) {
          Logger.error("Exception while searching for available  Qos Policies for the requirements: "
              + e.getMessage());
          throw new Exception("Cannot find an available  Qos Policies for requirements. Bandwidth Limit: "
              + bandwidthLimitInMbps + " - Minimum Bandwidth: " + minimumBandwidthInMbps);
        }
      }
      if (qosPolicy != null) {
        // add the qos to the port
        network.putProperty("qos_policy", qosPolicy);
      }

      model.addResource(network);
      HeatResource subnet = new HeatResource();
      subnet.setType("OS::Neutron::Subnet");
      subnet.setName("subnet." + link.getId());
      subnet.putProperty("name", "subnet." + link.getId());
      if (link.getCidr() != null) {
        subnet.putProperty("cidr", link.getCidr());
      } else {
        subnet.putProperty("subnetpool", myPool);
      }
      if (link.isDhcp() != null) {
        subnet.putProperty("enable_dhcp", link.isDhcp());
      }
      String[] dnsArray = {"8.8.8.8"};
      subnet.putProperty("dns_nameservers", dnsArray);
      HashMap<String, Object> netMap = new HashMap<String, Object>();
      netMap.put("get_resource", link.getId());
      subnet.putProperty("network", netMap);
      model.addResource(subnet);

      if ((link.isAccess() == null) || link.isAccess()) {
        // internal router interface for network
        HeatResource routerInterface = new HeatResource();
        routerInterface.setType("OS::Neutron::RouterInterface");
        routerInterface.setName("routerInterface." + link.getId());
        HashMap<String, Object> subnetMapInt = new HashMap<String, Object>();
        subnetMapInt.put("get_resource", "subnet." + link.getId());
        routerInterface.putProperty("subnet", subnetMapInt);
        routerInterface.putProperty("router", tenantExtRouter);
        model.addResource(routerInterface);
      }
    }

    model.prepare();

    Logger.debug("Created " + model.getResources().size() + " resources.");

    return model;
  }

  private void addSpAddressCloudConfigObject(VnfDescriptor vnfd, String instanceUuid,
                                             HeatModel model) {


    String sonataSpAddress = (String)AdaptorCore.getInstance().getSystemParameter("sonata_sp_address");

    HashMap<String, Object> fileToWrite = new HashMap<String,Object>();
    fileToWrite.put("path", "/etc/sonata_sp_address.conf");
    fileToWrite.put("content", "SP_ADDRESS="+sonataSpAddress+"\n");

    ArrayList<HashMap<String, Object>> filesToWrite = new ArrayList<HashMap<String, Object>>();
    filesToWrite.add(fileToWrite);

    HashMap<String, Object> spAddressCloudConfigMap = new HashMap<String, Object>();
    spAddressCloudConfigMap.put("write_files", filesToWrite);

    HeatResource spAddressCloudConfigObject = new HeatResource();
    spAddressCloudConfigObject.setType("OS::Heat::CloudConfig");
    spAddressCloudConfigObject.setName(vnfd.getName() + "_" + instanceUuid + "_spAddressCloudConfig");
    spAddressCloudConfigObject.putProperty("cloud_config", spAddressCloudConfigMap);
    model.addResource(spAddressCloudConfigObject);
  }

}
