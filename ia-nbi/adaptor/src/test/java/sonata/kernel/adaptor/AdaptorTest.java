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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import sonata.kernel.adaptor.commons.VimResources;
import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.messaging.TestConsumer;
import sonata.kernel.adaptor.messaging.TestProducer;
import sonata.kernel.adaptor.wrapper.WrapperBay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Unit test for simple App.
 */
public class AdaptorTest implements MessageReceiver {
  private String output = null;
  private Object mon = new Object();
  private TestConsumer northConsumer;
  private TestConsumer southConsumer;
  private String lastHeartbeat;

  @Before
  public void setUp() {
    System.setProperty("org.apache.commons.logging.Log",
        "org.apache.commons.logging.impl.SimpleLog");

    System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "false");

    System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "warn");

    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient",
        "warn");
  }

  /**
   * Register, send 4 heartbeat, deregister.
   * 
   * @throws IOException
   */
  @Test
  public void testHeartbeating() throws IOException {
    BlockingQueue<ServicePlatformMessage> northMuxQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();
    BlockingQueue<ServicePlatformMessage> southMuxQueue =
            new LinkedBlockingQueue<ServicePlatformMessage>();
    BlockingQueue<ServicePlatformMessage> northDispatcherQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();
    BlockingQueue<ServicePlatformMessage> southDispatcherQueue =
            new LinkedBlockingQueue<ServicePlatformMessage>();

    TestProducer northProducer = new TestProducer(northMuxQueue, this);
    northConsumer = new TestConsumer(northDispatcherQueue);
    TestProducer southProducer = new TestProducer(southMuxQueue, this);
    southConsumer = new TestConsumer(southDispatcherQueue);
    AdaptorCore core = new AdaptorCore(northMuxQueue, southMuxQueue, northDispatcherQueue, southDispatcherQueue, northConsumer, northProducer, southConsumer, southProducer,2);
    int counter = 0;

    core.start();
    Assert.assertNotNull(core.getUuid());

    try {
      while (counter < 4) {
        synchronized (mon) {
          mon.wait();
          if (lastHeartbeat.contains("RUNNING")) counter++;
        }
      }
    } catch (Exception e) {
      Assert.assertTrue(false);
    }

    System.out.println("Heartbeats received");
    Assert.assertTrue(true);

    core.stop();
    Assert.assertTrue(core.getState().equals("STOPPED"));
  }


  public void receiveHeartbeat(ServicePlatformMessage message) {
    synchronized (mon) {
      this.lastHeartbeat = message.getBody();
      mon.notifyAll();
    }
  }

  public void receive(ServicePlatformMessage message) {
    synchronized (mon) {
      this.output = message.getBody();
      mon.notifyAll();
    }
  }

  public void forwardToConsumer(ServicePlatformMessage message) {
    northConsumer.injectMessage(message);
  }
}
