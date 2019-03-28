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
 * @author Carlos Marques (ALB)
 * @author Michael Bredel (Ph.D.), NEC
 */

package sonata.kernel.adaptor;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ch.qos.logback.classic.util.ContextInitializer;

import sonata.kernel.adaptor.messaging.AbstractMsgBusConsumer;
import sonata.kernel.adaptor.messaging.AbstractMsgBusProducer;
import sonata.kernel.adaptor.messaging.MsgBusConsumer;
import sonata.kernel.adaptor.messaging.MsgBusProducer;
import sonata.kernel.adaptor.messaging.RabbitMqConsumer;
import sonata.kernel.adaptor.messaging.RabbitMqProducer;
import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.wrapper.VimRepo;
import sonata.kernel.adaptor.wrapper.WimRepo;
import sonata.kernel.adaptor.wrapper.WrapperBay;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;



public class AdaptorCore {

  public static final String APP_ID = "sonata.kernel.InfrAdaptor";
  private static AdaptorCore core;
  private static final String description = "Service Platform Infrastructure Adaptor";
  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(AdaptorCore.class);
  private static final String version = "0.0.1";
  private static final int writeLockCoolDown = 100000;
  private static final String SONATA_CONFIG_FILEPATH = "/etc/son-mano/sonata.config";
  private static AdaptorCore myInstance = null;
  private Properties sonataProperties;
  private static final String JERSEY_SERVLET_NAME = "jersey-container-servlet";

  
  public static AdaptorCore getInstance(){
   if (myInstance == null){
     myInstance = new AdaptorCore(0.1);
   } 
   return myInstance;
  }
  
  
  public Object getSystemParameter(String key){
    return sonataProperties.getProperty(key);
  }
  
  /**
   * Main method. param args the adaptor take no args.
   */
  public static void main(String[] args) throws IOException {
    // System.setProperty("log4j.logger.httpclient.wire.header", "WARN");
    // System.setProperty("log4j.logger.httpclient.wire.content", "WARN");

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();


    System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "/adaptor/src/main/resources/logback.xml");

    System.setProperty("org.apache.commons.logging.Log",
        "org.apache.commons.logging.impl.SimpleLog");

    System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "false");

    System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "warn");

    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient",
        "warn");
    
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        if(AdaptorCore.getInstance().getState().equals("RUNNNING"))
          AdaptorCore.getInstance().stop();
      }
    });
    AdaptorCore.getInstance().start();
    
  }

  private AdaptorDispatcherNorth northDispatcher;
  private AdaptorDispatcherSouth southDispatcher;
  private HeartBeat heartbeat;
  private AdaptorMux northMux;
  private AdaptorMux southMux;
  private MsgBusConsumer northConsumer;
  private MsgBusProducer northProducer;
  private MsgBusConsumer southConsumer;
  private MsgBusProducer southProducer;

  private double rate;
  private String registrationSid;
  private String status;


  private String uuid;

  private Object writeLock = new Object();

  /**
   * utility constructor for Tests. Allows attaching mock MsgBus to the adaptor plug-in Manager.
   * 
   * @param northMuxQueue A Java BlockingQueue for the AdaptorMux
   * @param southMuxQueue A Java BlockingQueue for the AdaptorMux for south
   * @param northDispatcherQueue A Java BlockingQueue for the AdaptorDispatcherNorth
   * @param southDispatcherQueue A Java BlockingQueue for the AdaptorDispatcherNorth for south
   * @param northConsumer The consumer queuing messages in the northDispatcher queue
   * @param northProducer The producer de-queuing messages from the mux queue
   * @param southConsumer The consumer queuing messages in the northDispatcher queue from south
   * @param southProducer The producer de-queuing messages from the mux queue for south
   * @param rate of the heart-beat in beat/s
   */
  public AdaptorCore(BlockingQueue<ServicePlatformMessage> northMuxQueue,
      BlockingQueue<ServicePlatformMessage> southMuxQueue,
      BlockingQueue<ServicePlatformMessage> northDispatcherQueue,
      BlockingQueue<ServicePlatformMessage> southDispatcherQueue, AbstractMsgBusConsumer northConsumer,
      AbstractMsgBusProducer northProducer, AbstractMsgBusConsumer southConsumer,
                     AbstractMsgBusProducer southProducer, double rate) {
    this.northMux = new AdaptorMux(northMuxQueue);
    this.southMux = new AdaptorMux(southMuxQueue);
    northDispatcher = new AdaptorDispatcherNorth(northDispatcherQueue, northMux, southMux,this);
    southDispatcher = new AdaptorDispatcherSouth(southDispatcherQueue, northMux, southMux);
    this.northConsumer = northConsumer;
    this.northProducer = northProducer;
    this.southConsumer = southConsumer;
    this.southProducer = southProducer;
    VimRepo vimRepo = new VimRepo();
    WrapperBay.getInstance().setVimRepo(vimRepo);
    WimRepo wimRepo = new WimRepo();
    WrapperBay.getInstance().setWimRepo(wimRepo);
    status = "READY";
    this.rate = rate;
    this.sonataProperties = parseConfigFile();
  }

  /**
   * Create an AdaptorCore ready to use. No services are started.
   * 
   * @param rate of the heart-beat in beat/s
   */
  private AdaptorCore(double rate) {
    
    this.sonataProperties = parseConfigFile();
    
    this.rate = rate;
    // instantiate the Adaptor:
    // - Mux and queue
    BlockingQueue<ServicePlatformMessage> northMuxQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();
    this.northMux = new AdaptorMux(northMuxQueue);

    BlockingQueue<ServicePlatformMessage> southMuxQueue =
            new LinkedBlockingQueue<ServicePlatformMessage>();
    this.southMux = new AdaptorMux(southMuxQueue);

    // - Dispatcher and queue
    BlockingQueue<ServicePlatformMessage> northDispatcherQueue =
        new LinkedBlockingQueue<ServicePlatformMessage>();
    BlockingQueue<ServicePlatformMessage> southDispatcherQueue =
            new LinkedBlockingQueue<ServicePlatformMessage>();
    northDispatcher = new AdaptorDispatcherNorth(northDispatcherQueue, northMux, southMux,this);
    southDispatcher = new AdaptorDispatcherSouth(southDispatcherQueue, northMux, southMux);
    // - Wrapper bay connection with the Database.
    VimRepo vimRepo = new VimRepo();
    WrapperBay.getInstance().setVimRepo(vimRepo);
    WimRepo wimRepo = new WimRepo();
    WrapperBay.getInstance().setWimRepo(wimRepo);
    // - Northbound interface

    this.northConsumer = new RabbitMqConsumer(northDispatcherQueue, "north");
    this.northProducer = new RabbitMqProducer(northMuxQueue,"north");

    // - Southbound interface

    this.southConsumer = new RabbitMqConsumer(southDispatcherQueue, "south");
    this.southProducer = new RabbitMqProducer(southMuxQueue,"south");

    status = "RUNNING";

  }

  /**
   * return the session ID of the registration message used to register this plugin to the
   * plugin-manager.
   * 
   * @return the session ID
   */
  public String getRegistrationSid() {
    return registrationSid;
  }

  /**
   * @return The status of this plug-in.
   */
  public String getState() {
    return this.status;
  }



  /**
   * @return this plug-in UUID.
   */
  public String getUuid() {
    return this.uuid;
  }

  /**
   * Handle the DeregistrationResponse message from the MANO Plugin Manager.
   * 
   * @param message the response message
   */
  public void handleDeregistrationResponse(ServicePlatformMessage message) {
    Logger.info("Received the deregistration response from the pluginmanager");
    JSONTokener tokener = new JSONTokener(message.getBody());
    JSONObject object = (JSONObject) tokener.nextValue();
    String status = object.getString("status");
    if (status.equals("OK")) {
      synchronized (writeLock) {
        writeLock.notifyAll();
      }
    } else {
      Logger.error("Failed to deregister to the plugin manager");
      this.status = "FAILED";
    }

  }

  /**
   * Handle the RegistrationResponse message from the MANO Plugin Manager.
   * 
   * @param message the response message
   */
  public void handleRegistrationResponse(ServicePlatformMessage message) {
    Logger.info("Received the registration response from the pluginmanager");
    JSONTokener tokener = new JSONTokener(message.getBody());
    JSONObject object = (JSONObject) tokener.nextValue();
    String status = object.getString("status");
    String pid = object.getString("uuid");
    if (status.equals("OK")) {
      synchronized (writeLock) {
        uuid = pid;
        writeLock.notifyAll();
      }
    } else {
      String error = object.getString("error");
      Logger.error("Failed to register to the plugin manager");
      Logger.error("Message: " + error);
    }

  }


  /**
   * Start the adaptor engines. Starts reading messages from the MsgBus
   * 
   * @throws IOException when something goes wrong in the MsgBus plug-in
   */
  public void start() throws IOException {
    // Start the message plug-in
    northProducer.connectToBus();
    northConsumer.connectToBus();
    northProducer.startProducing();
    northConsumer.startConsuming();

    southProducer.connectToBus();
    southConsumer.connectToBus();
    southProducer.startProducing();
    southConsumer.startConsuming();

    northDispatcher.start();
    southDispatcher.start();

    String port = (String) getSystemParameter("ia_api_port");
    if (port == null || port.isEmpty()) {
      port = "8083";
    }

    String contextPath = "";
    String appBase = ".";

    Tomcat tomcat = new Tomcat();
    tomcat.setPort(Integer.valueOf(port));
    tomcat.getHost().setAppBase(appBase);

    Context context = tomcat.addContext(contextPath, appBase);
    Tomcat.addServlet(context, JERSEY_SERVLET_NAME,
            new ServletContainer(new JerseyConfiguration()));
    context.addServletMappingDecoded("/api/ia/v1/*", JERSEY_SERVLET_NAME);

    try {
      tomcat.start();
    } catch (Exception e) {
      Logger.error(e.getMessage(), e);
    }
    //tomcat.getServer().await();

    register();
    status = "RUNNING";
    // - Start pumping blood
    this.heartbeat = new HeartBeat(northMux, rate, this);
    new Thread(this.heartbeat).start();

  }

  /**
   * Stop the engines: Message production and consumption, heart-beat.
   */
  public void stop() {
    this.deregister();
    this.heartbeat.stop();
    northProducer.stopProducing();
    northConsumer.stopConsuming();
    southProducer.stopProducing();
    southConsumer.stopConsuming();
    northDispatcher.stop();
    southDispatcher.stop();
  }

  private void deregister() {
    String body = "{\"uuid\":\"" + this.uuid + "\"}";
    String topic = "platform.management.plugin.deregister";
    ServicePlatformMessage message = new ServicePlatformMessage(body, "application/json", topic,
        java.util.UUID.randomUUID().toString(), topic);
    synchronized (writeLock) {
      try {
        this.registrationSid = message.getSid();
        northMux.enqueue(message);
        writeLock.wait(writeLockCoolDown);
      } catch (InterruptedException e) {
        Logger.error(e.getMessage(), e);
      }
    }
    this.status = "STOPPED";
  }

  private void register() {
    String body = "{\"name\":\"" + AdaptorCore.APP_ID + "\",\"version\":\"" + AdaptorCore.version
        + "\",\"description\":\"" + AdaptorCore.description + "\"}";
    String topic = "platform.management.plugin.register";
    ServicePlatformMessage message = new ServicePlatformMessage(body, "application/json", topic,
        java.util.UUID.randomUUID().toString(), topic);
    synchronized (writeLock) {
      try {
        this.registrationSid = message.getSid();
        northMux.enqueue(message);
        writeLock.wait(writeLockCoolDown);
      } catch (InterruptedException e) {
        Logger.error(e.getMessage(), e);
      }
    }
  }
  
  private static Properties parseConfigFile() {
    Logger.debug("Parsing sonata.config conf file");
    Properties prop = new Properties();
    try {
      InputStreamReader in =
          new InputStreamReader(new FileInputStream(SONATA_CONFIG_FILEPATH), Charset.forName("UTF-8"));

      JSONTokener tokener = new JSONTokener(in);

      JSONObject jsonObject = (JSONObject) tokener.nextValue();

      String brokerUrl = jsonObject.getString("sonata_sp_address");
      String apiPort = jsonObject.getString("ia_api_port");
      prop.put("sonata_sp_address", brokerUrl);
      prop.put("ia_api_port", apiPort);
    } catch (FileNotFoundException e) {
      Logger.error("Unable to load Broker Config file", e);
      System.exit(1);
    }
    Logger.debug("sonata.config conf file parsed");
    return prop;
  }
  
}

