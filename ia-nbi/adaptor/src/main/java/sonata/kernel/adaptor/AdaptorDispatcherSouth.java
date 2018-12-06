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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class AdaptorDispatcherSouth implements Runnable {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdaptorDispatcherSouth.class);
  private AdaptorMux northMux;
  private AdaptorMux southMux;
  private BlockingQueue<ServicePlatformMessage> mySouthQueue;
  private Executor myThreadPool;

  private boolean stop = false;

  /**
   * Create an AdaptorDispatcherNorth attached to the queue. CallProcessor will be bind to the provided
   * northMux.
   *
   * @param southQueue the queue the dispatcher is attached to south
   *
   * @param northMux the AdaptorMux the CallProcessors will be attached to
   * @param southMux the AdaptorMux the CallProcessors will be attached to south
   */
  public AdaptorDispatcherSouth(BlockingQueue<ServicePlatformMessage> southQueue,
                                AdaptorMux northMux, AdaptorMux southMux) {
    this.mySouthQueue = southQueue;
    myThreadPool = Executors.newCachedThreadPool();
    this.northMux = northMux;
    this.southMux = southMux;
  }

  @Override
  public void run() {
    ServicePlatformMessage message;
    do {

      try {
        message = mySouthQueue.take();

        if (message.getTopic().endsWith("compute.list")) {
          myThreadPool.execute(new FwListComputeVimCallProcessor(message, message.getSid(), northMux));
        } else if (message.getTopic().endsWith("prepare")) {
          myThreadPool.execute(new FwPrepareServiceCallProcessor(message, message.getSid(), northMux));
        } else if (message.getTopic().endsWith("service.remove")) {
          myThreadPool.execute(new FwRemoveServiceCallProcessor(message, message.getSid(), northMux));
        } else if (message.getTopic().endsWith("chain.deconfigure")) {
          myThreadPool.execute(new FwDeconfigureNetworkCallProcessor(message, message.getSid(), northMux));
        } else if (message.getTopic().endsWith("chain.configure")) {
          myThreadPool.execute(new FwConfigureNetworkCallProcessor(message, message.getSid(), northMux));
        } else {
          // Processor for fw packets from Southbound interface to Northbound interface
          myThreadPool.execute(new FwVimWimCallProcessor(message, message.getSid(), northMux));
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
}
