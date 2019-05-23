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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import sonata.kernel.adaptor.commons.*;
import org.slf4j.LoggerFactory;
import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.wrapper.*;

import java.util.ArrayList;
import java.util.Observable;

public class FwListComputeVimCallProcessor extends AbstractCallProcessor {
  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(FwListComputeVimCallProcessor.class);

  private ArrayList<String> vimVendors;

  /**
   * @param message
   * @param sid
   * @param mux
   */
  public FwListComputeVimCallProcessor(ServicePlatformMessage message, String sid, AdaptorMux mux) {
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
    String type = null;
    Logger.info("Call received - sid: " + message.getSid());

    ResourceRepo resourceRepo =  ResourceRepo.getInstance();
    VimVendor vimVendor = null;

    if (message.getTopic().contains(".heat.")) {
      vimVendor = ComputeVimVendor.HEAT;
      type = "vm";
    } else if (message.getTopic().contains(".mock.")) {
      vimVendor = ComputeVimVendor.MOCK;
      type = "vm";
    } else if (message.getTopic().contains(".k8s.")) {
      vimVendor = ComputeVimVendor.K8S;
      type = "container";
    }

    if ((vimVendor == null) || (type == null)) {
      return false;
    }

    String body = null;
    ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
    ArrayList<VimResources> resList = new ArrayList<>();
    try {
      VimResourcesList payload = mapper.readValue(message.getBody(), VimResourcesList.class);
      if (!payload.getResources().isEmpty()) {
        for (VimResources resource : payload.getResources()) {
          resource.setType(type);
          resList.add(resource);
        }
        VimResourcesList responseBody = new VimResourcesList();
        responseBody.setResources(resList);
        body = mapper.writeValueAsString(responseBody);
      } else {
        body = message.getBody();
      }
    } catch (Exception e) {
      Logger.error("Error parsing the payload: " + e.getMessage(), e);
      return false;
    }

    //Logger.debug("Content modified: " + body);
    synchronized (resourceRepo) {

      if (resourceRepo.putResourcesForRequestIdAndVendor(message.getSid(),vimVendor,body)) {
        if (resourceRepo.getStoredVendorsNumberForRequestId(message.getSid())
                .equals(resourceRepo.getExpectedVendorsNumberForRequestId(message.getSid()))) {
          try {
            Logger.info(
                    message.getSid().substring(0, 10) + " - Forward message to northbound interface.");

            //message.setTopic(message.getTopic().replace("nbi.",""));

            ArrayList<String> content= resourceRepo.getResourcesFromRequestId(message.getSid());

            VimResourcesList data = null;
            ArrayList<VimResources> vimList = new ArrayList<>();

            try {
              for (String value : content) {
                data = mapper.readValue(value, VimResourcesList.class);
                vimList.addAll(data.getResources());
              }
            } catch (Exception e) {
              Logger.error("Error parsing the payload: " + e.getMessage(), e);
              return false;
            }

            ArrayList<NepResources> nepList = new ArrayList<>();
            ArrayList<String> nepsUuid = WrapperBay.getInstance().getNepList();

            if (nepsUuid != null) {
              Logger.debug(nepsUuid.toString());

              for(String nep : nepsUuid){
                VimWrapperConfiguration config = WrapperBay.getInstance().getConfig(nep);

                if(config==null){
                  continue;
                }
                NepResources bodyElement = new NepResources();
                bodyElement.setNepUuid(config.getUuid());
                bodyElement.setNepName(config.getName());
                bodyElement.setType(config.getWrapperType().toString());

                nepList.add(bodyElement);
              }
            }

            ComputeListResponse computeListResponse = new ComputeListResponse();
            computeListResponse.setVimList(vimList);
            computeListResponse.setNepList(nepList);

            // Need a new mapper different from SonataManifestMapper for allow feature WRITE_EMPTY_JSON_ARRAYS
            ObjectMapper mapperW = new ObjectMapper(new YAMLFactory());
            mapperW.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
            mapperW.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
            mapperW.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            String finalBody;
            finalBody = mapperW.writeValueAsString(computeListResponse);

            //Logger.debug("Final Content: " + finalBody);
            ServicePlatformMessage response = new ServicePlatformMessage(finalBody, "application/json",
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
