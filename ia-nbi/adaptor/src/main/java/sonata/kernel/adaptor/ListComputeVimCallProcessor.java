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

package sonata.kernel.adaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.LoggerFactory;

import sonata.kernel.adaptor.commons.ManagementComputeListResponse;
import sonata.kernel.adaptor.commons.SonataManifestMapper;
import sonata.kernel.adaptor.commons.VimResources;
import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.wrapper.ResourceRepo;

import java.util.ArrayList;
import java.util.Observable;

public class ListComputeVimCallProcessor extends AbstractCallProcessor {

  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(ListComputeVimCallProcessor.class);

  private int vendorSize;

  public ListComputeVimCallProcessor(ServicePlatformMessage message, String sid, AdaptorMux mux, int vendorSize) {
    super(message, sid, mux);
    this.vendorSize = vendorSize;
  }

  @Override
  public boolean process(ServicePlatformMessage message) {
    Logger.info("Wait for replys from Compute Wrappers or timeout for north");

    ResourceRepo resourceRepo =  ResourceRepo.getInstance();
    synchronized (resourceRepo) {
      resourceRepo.putResourcesForRequestId(message.getSid(),vendorSize);
    }

    int wait = 15000;
    try {
      Thread.sleep(wait);
    } catch (InterruptedException e) {
      Logger.error(e.getMessage(), e);
    }

    boolean status = false;
    synchronized (resourceRepo) {
      if (resourceRepo.getStatusResourcesFromRequestId(message.getSid())) {
        if (resourceRepo.getStoredVendorsNumberForRequestId(message.getSid())>0) {
          // Try send partial reply to north
          try {
            Logger.info(
                    message.getSid().substring(0, 10) + " - Forward message to northbound interface.");

            ArrayList<String> content= resourceRepo.getResourcesFromRequestId(message.getSid());

            ManagementComputeListResponse data = null;
            ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
            ArrayList<VimResources> finalContent = new ArrayList<>();

            try {
              for (String value : content) {
                //Logger.debug("Data: " + value);
                data = mapper.readValue(value, ManagementComputeListResponse.class);
                finalContent.addAll(data.getResources());
              }
            } catch (Exception e) {
              Logger.error("Error parsing the payload: " + e.getMessage(), e);
              finalContent = null;
            }

            if (finalContent != null) {
              String body;
              body = mapper.writeValueAsString(finalContent);
              //Logger.debug("Final Content: " + body);

              ServicePlatformMessage response = new ServicePlatformMessage(body, "application/json",
                      message.getReplyTo(), message.getSid(), null);
              this.sendToMux(response);
            } else {
              status = true;
            }

            resourceRepo.removeResourcesFromRequestId(message.getSid());

          } catch (Exception e) {
            Logger.error("Error redirecting the message: " + e.getMessage(), e);
            status = true;
          }
        } else {
          resourceRepo.removeResourcesFromRequestId(message.getSid());
          status = true;
        }
      }
    }

    if (status) {
      Logger.info("Timeout Error in List Compute Vim Call.");
      ServicePlatformMessage response = new ServicePlatformMessage(
              "{\"request_status\":\"ERROR\",\"message\":\"Timeout Error in List Compute Vim Call\"}",
              "application/json", this.getMessage().getReplyTo().replace("nbi.",""), this.getSid(), null);
      this.getMux().enqueue(response);
      return false;
    }

    return true;
  }

  @Override
  public void update(Observable obs, Object arg) {
    // This call does not need to be updated by any observable (wrapper).
  }

}
