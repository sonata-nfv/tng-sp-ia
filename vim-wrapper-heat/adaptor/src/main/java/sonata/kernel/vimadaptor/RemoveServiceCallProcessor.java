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

package sonata.kernel.vimadaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.LoggerFactory;

import sonata.kernel.vimadaptor.commons.ServiceRemovePayload;
import sonata.kernel.vimadaptor.commons.SonataManifestMapper;
import sonata.kernel.vimadaptor.messaging.ServicePlatformMessage;
import sonata.kernel.vimadaptor.wrapper.ComputeWrapper;
import sonata.kernel.vimadaptor.wrapper.WrapperBay;
import sonata.kernel.vimadaptor.wrapper.WrapperStatusUpdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;

public class RemoveServiceCallProcessor extends AbstractCallProcessor {

  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(RemoveServiceCallProcessor.class);
  private ServiceRemovePayload data;

  /**
   * Generate a CallProcessor to process an API call to create a new VIM wrapper
   * 
   * @param message the API call message
   * @param sid the session ID of thi API call
   * @param mux the Adaptor Mux to which send responses.
   */
  public RemoveServiceCallProcessor(ServicePlatformMessage message, String sid, AdaptorMux mux) {
    super(message, sid, mux);
  }

  @Override
  public boolean process(ServicePlatformMessage message) {
    boolean out = true;
    Logger.info("Remove service call received by call processor.");
    // parse the payload to get Wrapper UUID from the request body
    Logger.info("Parsing payload...");
    data = null;
    ObjectMapper mapper = SonataManifestMapper.getSonataMapper();

    try {
      data = mapper.readValue(message.getBody(), ServiceRemovePayload.class);
      Logger.info("payload parsed");
      ComputeWrapper wr = WrapperBay.getInstance().getComputeWrapper(data.getVimUuid());
      Logger.info("Wrapper retrieved");

      if (wr == null) {
        Logger.warn("Error retrieving the wrapper");

        this.sendToMux(new ServicePlatformMessage(
            "{\"request_status\":\"ERROR\",\"message\":\"VIM not found\"}", "application/json",
            message.getReplyTo(), message.getSid(), null));
        out = false;
      } else {
        Logger.info(
            "Calling wrapper: " + wr.getConfig().getName() + "- UUID: " + wr.getConfig().getUuid());
        wr.addObserver(this);
        wr.removeService(data, this.getSid());
      }
    } catch (Exception e) {
      Logger.error("Error Removing the service: " + e.getMessage(), e);
      this.sendToMux(new ServicePlatformMessage(
          "{\"request_status\":\"ERROR\",\"message\":\"Removing Error\"}", "application/json",
          message.getReplyTo(), message.getSid(), null));
      out = false;
    }
    return out;

  }

  @Override
  public void update(Observable observable, Object arg) {

    WrapperStatusUpdate update = (WrapperStatusUpdate) arg;
    if (update.getSid() == null) {
      Logger.warn("Wrapper Update message with no SID.");
      return;
    }
    if (!update.getSid().equals(this.getSid())) return;
    Logger.info("Received an update:\n" + update.getBody());

    String updateStatus = update.getStatus();

    if (updateStatus.equals("SUCCESS")) {
      Logger.debug("Service Successfully Removed.");
      sendResponse("{\"request_status\":\"COMPLETED\",\"message\":\"\"}");
    } else {
      JSONTokener tokener = new JSONTokener(update.getBody());
      JSONObject jsonObject = (JSONObject) tokener.nextValue();
      String status = jsonObject.getString("status");
      Logger.error("Error removing the service. " + status);
      sendResponse("{\"request_status\":\"ERROR\",\"message\":\"" + update.getBody() + "\"}");
    }

  }

  private void sendResponse(String message) {
    if (this.getMessage().getReplyTo() != null) {
      ServicePlatformMessage spMessage = new ServicePlatformMessage(message, "application/json",
          this.getMessage().getReplyTo(), this.getMessage().getSid(), this.getMessage().getTopic());
      this.sendToMux(spMessage);
    }
  }
}
