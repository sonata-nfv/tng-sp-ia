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
 * @author Carlos Marques (ALB)
 * 
 */
package sonata.kernel.adaptor;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.LoggerFactory;
import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.wrapper.ComputeVimVendor;
import sonata.kernel.adaptor.wrapper.ResourceRepo;
import sonata.kernel.adaptor.wrapper.VimVendor;

import java.util.ArrayList;
import java.util.Observable;

public class FwPrepareServiceCallProcessor extends AbstractCallProcessor {
  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(FwPrepareServiceCallProcessor.class);

  private ArrayList<String> vimVendors;

  /**
   * @param message
   * @param sid
   * @param mux
   */
  public FwPrepareServiceCallProcessor(ServicePlatformMessage message, String sid, AdaptorMux mux) {
    super(message, sid, mux);
  }

  /*
   * (non-Javadoc)
   * 
   * @see sonata.kernel.adaptor.AbstractCallProcessor#process(sonata.kernel.adaptor.messaging.
   * ServicePlatformMessage)
   */
  @Override
  public boolean process(ServicePlatformMessage message) {

    boolean out = true;
    Logger.info("Call received - sid: " + message.getSid());

    ResourceRepo resourceRepo =  ResourceRepo.getInstance();
    VimVendor vimVendor = null;

    if (message.getTopic().contains(".heat.")) {
      vimVendor = ComputeVimVendor.HEAT;
    } else if (message.getTopic().contains(".mock.")) {
      vimVendor = ComputeVimVendor.MOCK;
    } else if (message.getTopic().contains(".k8s.")) {
      vimVendor = ComputeVimVendor.K8S;
    }

    if (vimVendor == null) {
      return false;
    }

    synchronized (resourceRepo) {

      if (resourceRepo.putResourcesForRequestIdAndVendor(message.getSid(),vimVendor,message.getBody())) {
        if (resourceRepo.getStoredVendorsNumberForRequestId(message.getSid())
                .equals(resourceRepo.getExpectedVendorsNumberForRequestId(message.getSid()))) {
          try {
            Logger.info(
                    message.getSid().substring(0, 10) + " - Forward message to northbound interface.");

            //message.setTopic(message.getTopic().replace("nbi.",""));

            ArrayList<String> content= resourceRepo.getResourcesFromRequestId(message.getSid());
            String body = null;

            try {
              for (String value : content) {
                JSONTokener tokener = new JSONTokener(value);
                JSONObject jsonObject = (JSONObject) tokener.nextValue();
                String requestStatus = null;
                try {
                  requestStatus = jsonObject.getString("request_status");
                  if ((body == null) || !requestStatus.equals("COMPLETED")) {
                    body = value;
                  }
                } catch (Exception e) {
                  Logger.error("Error getting the request_status: " + e.getMessage(), e);
                  return false;
                }

              }
            } catch (Exception e) {
              Logger.error("Error parsing the payload: " + e.getMessage(), e);
              return false;
            }

            ServicePlatformMessage response = new ServicePlatformMessage(body, "application/json",
                    message.getTopic().replace("nbi.infrastructure."+vimVendor.toString()+".","infrastructure."), message.getSid(), null);

            resourceRepo.removeResourcesFromRequestId(message.getSid());

            this.sendToMux(response);
          } catch (Exception e) {
            Logger.error("Error redirecting the message: " + e.getMessage(), e);
            out = false;
          }
        }
      } else {
        out = false;
      }

    }

    return out;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
   */
  @Override
  public void update(Observable arg0, Object arg1) {
    // TODO Auto-generated method stub

  }

}
