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
import org.slf4j.LoggerFactory;
import sonata.kernel.vimadaptor.commons.ServiceNetworkCreatePayload;
import sonata.kernel.vimadaptor.commons.ServiceNetworkDeletePayload;
import sonata.kernel.vimadaptor.commons.SonataManifestMapper;
import sonata.kernel.vimadaptor.commons.VimPreDeploymentList;
import sonata.kernel.vimadaptor.messaging.ServicePlatformMessage;
import sonata.kernel.vimadaptor.wrapper.ComputeWrapper;
import sonata.kernel.vimadaptor.wrapper.WrapperBay;

import java.util.Observable;

public class NetworkDeleteServiceCallProcessor extends AbstractCallProcessor {
  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(NetworkDeleteServiceCallProcessor.class);

  /**
   * @param message
   * @param sid
   * @param mux
   */
  public NetworkDeleteServiceCallProcessor(ServicePlatformMessage message, String sid, AdaptorMux mux) {
    super(message, sid, mux);
  }

  /*
   * (non-Javadoc)
   * 
   * @see sonata.kernel.vimadaptor.AbstractCallProcessor#process(sonata.kernel.vimadaptor.messaging.
   * ServicePlatformMessage)
   */
  @Override
  public boolean process(ServicePlatformMessage message) {

    boolean out = true;
    Logger.info("Call received - sid: " + message.getSid());
    // parse the payload to get Wrapper UUID and NSD/VNFD from the request body
    Logger.info("Parsing payload...");
    ServiceNetworkDeletePayload payload = null;
    ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
    // ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    // SimpleModule module = new SimpleModule();
    // module.addDeserializer(Unit.class, new UnitDeserializer());
    // //module.addDeserializer(VmFormat.class, new VmFormatDeserializer());
    // //module.addDeserializer(ConnectionPointType.class, new ConnectionPointTypeDeserializer());
    // mapper.registerModule(module);
    // mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    // mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    try {
      payload = mapper.readValue(message.getBody(), ServiceNetworkDeletePayload.class);
      Logger.info("payload parsed. Configuring VIMs");

      for (VimPreDeploymentList vim : payload.getVimList()) {
        ComputeWrapper wr = WrapperBay.getInstance().getComputeWrapper(vim.getUuid());
        if (wr == null) {
          continue;
        }
        Logger.info(message.getSid().substring(0, 10) + " - Wrapper retrieved");

        if (WrapperBay.getInstance().getVimRepo().getServiceInstanceVimUuid(payload.getInstanceId(),
            vim.getUuid()) == null) {

          throw new Exception("Service needs to be prepared first for instance "
                  + payload.getInstanceId() + " on Compute VIM " + vim.getUuid());
        } else {
          boolean success = wr.networkDelete(payload.getInstanceId(), vim.getVirtualLinks());
          if (!success) {
            throw new Exception("Unable to delete the network for instance: "
                    + payload.getInstanceId() + " on Compute VIM " + vim.getUuid());
          }
        }

      }
      Logger.info(
          message.getSid().substring(0, 10) + " - Network delete complete. Sending back response.");
      String responseJson = "{\"request_status\":\"COMPLETED\",\"message\":\"\"}";
      ServicePlatformMessage responseMessage = new ServicePlatformMessage(responseJson,
          "application/json", message.getReplyTo(), message.getSid(), this.getMessage().getTopic());
      this.sendToMux(responseMessage);

    } catch (Exception e) {
      Logger.error("Error in network delete: " + e.getMessage(), e);
      this.sendToMux(new ServicePlatformMessage(
          "{\"request_status\":\"ERROR\",\"message\":\""
              + e.getMessage().replace("\"", "''").replace("\n", "") + "\"}",
          "application/json", message.getReplyTo(), message.getSid(), this.getMessage().getTopic()));
      out = false;
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
