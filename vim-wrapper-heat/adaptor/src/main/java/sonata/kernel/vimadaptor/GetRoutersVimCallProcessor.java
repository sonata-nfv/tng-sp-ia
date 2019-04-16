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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.LoggerFactory;
import sonata.kernel.vimadaptor.commons.HeatRequestPayload;
import sonata.kernel.vimadaptor.commons.SonataManifestMapper;
import sonata.kernel.vimadaptor.messaging.ServicePlatformMessage;
import sonata.kernel.vimadaptor.wrapper.*;
import sonata.kernel.vimadaptor.wrapper.openstack.Router;

import java.util.ArrayList;
import java.util.Observable;

public class GetRoutersVimCallProcessor extends AbstractCallProcessor {

  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(GetRoutersVimCallProcessor.class);

  public GetRoutersVimCallProcessor(ServicePlatformMessage message, String sid, AdaptorMux mux) {
    super(message, sid, mux);
  }

  @Override
  public boolean process(ServicePlatformMessage message) {
    Logger.info("Retrieving Routers from VIM");
    ArrayList<Router> routersList = new ArrayList<>();

    ObjectMapper mapperSon = SonataManifestMapper.getSonataJsonMapper();

    try {
      HeatRequestPayload heatReq = mapperSon.readValue(message.getBody(), HeatRequestPayload.class);


      WrapperConfiguration config = new WrapperConfiguration();
      config.setUuid(heatReq.getUuid());
      config.setWrapperType(WrapperType.COMPUTE);
      config.setVimVendor(ComputeVimVendor.HEAT);
      config.setVimEndpoint(heatReq.getEndpoint());
      config.setAuthUserName(heatReq.getUserName());
      config.setAuthPass(heatReq.getPassword());
      config.setDomain(heatReq.getDomain());
      config.setConfiguration("{" + "\"tenant\":\"" + heatReq.getTenant() + "\"," +  "\"tenant_ext_net\":\"" + heatReq.getExternalNetworkId() + "\""  + "}");


      ComputeWrapper wr = (ComputeWrapper) WrapperFactory.createWrapper(config);

      ArrayList<Router> routers = wr.getRouters();

      if (routers != null) {
        routersList = routers;
      }
    } catch (Exception e) {
      Logger.error("Error getting the routers: " + e.getMessage(), e);
      ServicePlatformMessage response = new ServicePlatformMessage(
          "{\"request_status\":\"ERROR\",\"message\":\"Error getting the routers\"}",
          "application/json", this.getMessage().getReplyTo(), this.getSid(), this.getMessage().getTopic());
      this.getMux().enqueue(response);
      return false;
    }

    // Create the response

    // Need a new mapper different from SonataManifestMapper for allow feature WRITE_EMPTY_JSON_ARRAYS
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    mapper.setSerializationInclusion(Include.NON_NULL);

    String body;
    try {

      Logger.info("Sending back response...");
      body = mapper.writeValueAsString(routersList);
      //body = mapper.writeValueAsString(routersList);

      ServicePlatformMessage response = new ServicePlatformMessage(body, "application/json",
          this.getMessage().getReplyTo(), this.getSid(), this.getMessage().getTopic());

      this.getMux().enqueue(response);
      Logger.info("Get Routers call completed.");
      return true;
    } catch (JsonProcessingException e) {
      ServicePlatformMessage response = new ServicePlatformMessage(
          "{\"request_status\":\"ERROR\",\"message\":\"Internal Server Error in Routers call\"}",
          "application/json", this.getMessage().getReplyTo(), this.getSid(), this.getMessage().getTopic());
      this.getMux().enqueue(response);
      return false;
    }
  }

  @Override
  public void update(Observable obs, Object arg) {
    // This call does not need to be updated by any observable (wrapper).
  }

}
