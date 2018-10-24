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
 * @author Thomas Soenen, imec
 *
 * @author Michael Bredel (Ph.D.), NEC
 * @author Carlos Marques (ALB)
 */

package sonata.kernel.adaptor;

import org.slf4j.LoggerFactory;

import sonata.kernel.adaptor.messaging.ServicePlatformMessage;

import sonata.kernel.adaptor.commons.GetVimVendors;
import sonata.kernel.adaptor.commons.GetWimVendors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.ArrayList;


public class AdaptorDispatcherNorth implements Runnable {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdaptorDispatcherNorth.class);
  private AdaptorCore core;
  private AdaptorMux northMux;
  private AdaptorMux southMux;
  private BlockingQueue<ServicePlatformMessage> myNorthQueue;
  private Executor myThreadPool;
  private GetVimVendors getVimVendors;
  private GetWimVendors getWimVendors;

  private boolean stop = false;

  /**
   * Create an AdaptorDispatcherNorth attached to the queue. CallProcessor will be bind to the provided
   * northMux.
   * 
   * @param northQueue the queue the dispatcher is attached to
   * 
   * @param northMux the AdaptorMux the CallProcessors will be attached to
   * @param southMux the AdaptorMux the CallProcessors will be attached to south
   */
  public AdaptorDispatcherNorth(BlockingQueue<ServicePlatformMessage> northQueue,
                                AdaptorMux northMux, AdaptorMux southMux, AdaptorCore core) {
    this.myNorthQueue = northQueue;
    myThreadPool = Executors.newCachedThreadPool();
    this.northMux = northMux;
    this.southMux = southMux;
    this.core = core;
    this.getVimVendors = new GetVimVendors();
    this.getWimVendors = new GetWimVendors();
  }

  @Override
  public void run() {
    ServicePlatformMessage message;
    do {
      try {
        message = myNorthQueue.take();

        if (isRegistrationResponse(message)) {
          this.core.handleRegistrationResponse(message);
        } else if (isDeregistrationResponse(message)) {
          this.core.handleDeregistrationResponse(message);
        } else if (isWanMessage(message)) {
          handleWanMessage(message);
        } else if (isManagementMsg(message)) {
          handleManagementMessage(message);
        } else if (isServiceMsg(message)) {
          this.handleServiceMsg(message);
        } else if (isFunctionMessage(message)) {
          handleFunctionMessage(message);
        } else if (isMonitoringMessage(message)) {
          this.handleMonitoringMessage(message);
        }

      } catch (InterruptedException e) {
        Logger.error(e.getMessage(), e);
      }

    } while (!stop);
  }


  public void start() {
    Thread thread = new Thread(this);
    thread.start();
  }

  public void stop() {
    this.stop = true;
  }

  private void handleFunctionMessage(ServicePlatformMessage message) {
    ArrayList<String> vimVendors = null;
    // Redirect VIM
    if (message.getTopic().endsWith("deploy")) {
      vimVendors = this.getVimVendors.GetVimVendors(message,"deploy");
    } else if (message.getTopic().endsWith("scale")) {
      vimVendors = this.getVimVendors.GetVimVendors(message,"scale");
    } else if (message.getTopic().endsWith("remove")) {
      vimVendors = this.getVimVendors.GetVimVendors(message, "function.remove");
    }
    if (vimVendors == null) {
      this.northMux.enqueue(new ServicePlatformMessage(
              "{\"request_status\":\"ERROR\",\"message\":\""
                      + "Error retrieving the Vims Type" + "\"}",
              "application/json", message.getReplyTo(), message.getSid(), null));
    } else {
      myThreadPool.execute(new RedirectVimWimCallProcessor(message, message.getSid(), southMux, vimVendors));
    }

  }

  private void handleManagementMessage(ServicePlatformMessage message) {

    if (message.getTopic().contains("compute")) { // compute menagement API
      if (message.getTopic().endsWith("add")) {
        myThreadPool.execute(new AddVimCallProcessor(message, message.getSid(), northMux));
      } else if (message.getTopic().endsWith("remove")) {
        myThreadPool.execute(new RemoveVimCallProcessor(message, message.getSid(), northMux));
      } else if (message.getTopic().endsWith("resourceAvailability")) {
        myThreadPool.execute(new ResourceAvailabilityCallProcessor(message, message.getSid(), northMux));
      } else if (message.getTopic().endsWith("list")) {
        Logger.info("Received a \"List VIMs\" API call on topic: " + message.getTopic());
        ArrayList<String> vimVendors = this.getVimVendors.GetVimVendors(message, "compute.list");
        if (vimVendors == null) {
          this.northMux.enqueue(new ServicePlatformMessage(
                  "{\"request_status\":\"ERROR\",\"message\":\""
                          + "Error retrieving the Vims Type" + "\"}",
                  "application/json", message.getReplyTo(), message.getSid(), null));
        } else {
          myThreadPool.execute(new ListComputeVimCallProcessor(message, message.getSid(), northMux, vimVendors.size()));
          myThreadPool.execute(new RedirectVimWimCallProcessor(message, message.getSid(), southMux, vimVendors));
        }
      }
    } else if (message.getTopic().contains("storage")) {
      // TODO Storage Management API
    } else if (message.getTopic().contains("network")) {
      if (message.getTopic().endsWith("add")) {
        myThreadPool.execute(new AddVimCallProcessor(message, message.getSid(), northMux));
      } else if (message.getTopic().endsWith("remove")) {
        myThreadPool.execute(new RemoveVimCallProcessor(message, message.getSid(), northMux));
      } else if (message.getTopic().endsWith("list")) {
        Logger.info("Received a \"List VIMs\" API call on topic: " + message.getTopic());
        myThreadPool.execute(new ListNetworkVimCallProcessor(message, message.getSid(), northMux));
      } else {
        Logger.info("Received an unknown menagement API call on topic: " + message.getTopic());
      }
    }

  }

  private void handleMonitoringMessage(ServicePlatformMessage message) {
    if (message.getTopic().contains("compute")) {
      Logger.info("Received a \"monitoring\" API call on topic: " + message.getTopic());
    } else if (message.getTopic().contains("storage")) {
      Logger.info("Received a \"monitoring\" API call on topic: " + message.getTopic());
    } else if (message.getTopic().contains("network")) {
      Logger.info("Received a \"monitoring\" API call on topic: " + message.getTopic());
    }
  }

  private void handleServiceMsg(ServicePlatformMessage message) {
    ArrayList<String> vimVendors = null;
    // Redirect VIM
    if (message.getTopic().endsWith("remove")) {
      Logger.info("Received a \"service.remove\" API call on topic: " + message.getTopic());
      vimVendors = this.getVimVendors.GetVimVendors(message,"service.remove");
    } else if (message.getTopic().endsWith("prepare")) {
      Logger.info("Received a \"service.prepare\" API call on topic: " + message.getTopic());
      vimVendors = this.getVimVendors.GetVimVendors(message,"prepare");
    } else if (message.getTopic().endsWith("chain.deconfigure")) {
      Logger.info("Received a \"Network\" API call on topic: " + message.getTopic());
      vimVendors = this.getVimVendors.GetVimVendors(message,"chain.deconfigure");
    } else if (message.getTopic().endsWith("chain.configure")) {
      Logger.info("Received a \"Network\" API call on topic: " + message.getTopic());
      vimVendors = this.getVimVendors.GetVimVendors(message, "chain.configure");
    }

    if (vimVendors == null) {
      this.northMux.enqueue(new ServicePlatformMessage(
              "{\"request_status\":\"ERROR\",\"message\":\""
                      + "Error retrieving the Vims Type" + "\"}",
              "application/json", message.getReplyTo(), message.getSid(), null));
    } else {
      if (message.getTopic().endsWith("remove")) {
        myThreadPool.execute(new RemoveServiceCallProcessor(message, message.getSid(), northMux, vimVendors.size()));
      } else if (message.getTopic().endsWith("prepare")) {
        myThreadPool.execute(new PrepareServiceCallProcessor(message, message.getSid(), northMux, vimVendors.size()));
      } else if (message.getTopic().endsWith("chain.deconfigure")) {
        myThreadPool.execute(new DeconfigureNetworkCallProcessor(message, message.getSid(), northMux, vimVendors.size()));
      } else if (message.getTopic().endsWith("chain.configure")) {
        myThreadPool.execute(new ConfigureNetworkCallProcessor(message, message.getSid(), northMux, vimVendors.size()));
      }
      myThreadPool.execute(new RedirectVimWimCallProcessor(message, message.getSid(), southMux, vimVendors));
    }

  }

  private void handleWanMessage(ServicePlatformMessage message) {

    if (message.getTopic().endsWith("wan.add")) {
      myThreadPool.execute(new AddWimCallProcessor(message, message.getSid(), northMux));
    } else if (message.getTopic().endsWith("wan.remove")) {
      myThreadPool.execute(new RemoveWimCallProcessor(message, message.getSid(), northMux));
    } else if (message.getTopic().endsWith("wan.list")) {
      myThreadPool.execute(new ListWimCallProcessor(message, message.getSid(), northMux));
    } else if (message.getTopic().endsWith("wan.attach")){
      myThreadPool.execute(new AttachVimCallProcessor(message, message.getSid(), northMux));
    } else if (message.getTopic().endsWith("wan.configure")) {
      // Redirect WIM
      ArrayList<String> wimVendors = this.getWimVendors.GetWimVendors(message,"configure");
      if (wimVendors == null) {
        this.northMux.enqueue(new ServicePlatformMessage(
                "{\"request_status\":\"ERROR\",\"message\":\""
                        + "Error retrieving the Wims Type" + "\"}",
                "application/json", message.getReplyTo(), message.getSid(), null));
      } else {
        myThreadPool.execute(new RedirectVimWimCallProcessor(message, message.getSid(), southMux, wimVendors));
      }
    }else if (message.getTopic().endsWith("wan.deconfigure")) {
      // Redirect WIM
      ArrayList<String> wimVendors = this.getWimVendors.GetWimVendors(message,"deconfigure");
      if (wimVendors == null) {
        //this.northMux.enqueue(new ServicePlatformMessage(
        //        "{\"request_status\":\"ERROR\",\"message\":\""
        //                + "Error retrieving the Wims Type" + "\"}",
        //        "application/json", message.getReplyTo(), message.getSid(), null));
        Logger.warn("WimVendors not found, continue");
      } else {
        myThreadPool.execute(new RedirectVimWimCallProcessor(message, message.getSid(), southMux, wimVendors));
      }
    }

  }

  private boolean isDeregistrationResponse(ServicePlatformMessage message) {
    return message.getTopic().equals("platform.management.plugin.deregister")
        && message.getSid().equals(core.getRegistrationSid());
  }

  private boolean isFunctionMessage(ServicePlatformMessage message) {
    return message.getTopic().contains("infrastructure.function");
  }

  private boolean isManagementMsg(ServicePlatformMessage message) {
    return message.getTopic().contains("infrastructure.management");
  }

  private boolean isMonitoringMessage(ServicePlatformMessage message) {
    return message.getTopic().contains("infrastructure.monitoring");
  }

  private boolean isRegistrationResponse(ServicePlatformMessage message) {
    return message.getTopic().equals("platform.management.plugin.register")
        && message.getSid().equals(core.getRegistrationSid());
  }

  private boolean isServiceMsg(ServicePlatformMessage message) {
    return message.getTopic().contains("infrastructure.service");
  }

  private boolean isWanMessage(ServicePlatformMessage message) {
    return message.getTopic().contains(".wan.");
  }
}
