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

import org.slf4j.LoggerFactory;
import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.wrapper.ResourceRepo;

import java.util.Observable;

public class RemoveServiceCallProcessor extends AbstractCallProcessor {
  private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(RemoveServiceCallProcessor.class);

  private int vendorSize;

  /**
   * @param message
   * @param sid
   * @param mux
   */
  public RemoveServiceCallProcessor(ServicePlatformMessage message, String sid, AdaptorMux mux, int vendorSize) {
    super(message, sid, mux);
    this.vendorSize = vendorSize;
  }

  /*
   * (non-Javadoc)
   * 
   * @see sonata.kernel.vimadaptor.AbstractCallProcessor#process(sonata.kernel.vimadaptor.messaging.
   * ServicePlatformMessage)
   */
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
      // if someone don't respond the process give timeout
      if (resourceRepo.getStatusResourcesFromRequestId(message.getSid())) {
        resourceRepo.removeResourcesFromRequestId(message.getSid());
        status = true;
      }
    }

    if (status) {
      Logger.info("Timeout Error in Remove Service Call.");
      ServicePlatformMessage response = new ServicePlatformMessage(
              "{\"request_status\":\"ERROR\",\"message\":\"Timeout Error in Remove Service Call\"}",
              "application/json", this.getMessage().getReplyTo(), this.getSid(), null);
      this.getMux().enqueue(response);
      return false;
    }

    return true;


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
