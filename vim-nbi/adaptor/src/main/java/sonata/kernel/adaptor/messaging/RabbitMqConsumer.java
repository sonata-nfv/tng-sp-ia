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
 */

package sonata.kernel.adaptor.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

public class RabbitMqConsumer extends AbstractMsgBusConsumer implements MsgBusConsumer, Runnable {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(RabbitMqConsumer.class);
  private Channel channel;
  // private Connection connection;
  private String queueName;

  DefaultConsumer consumer;
  private String direction;

  public RabbitMqConsumer(BlockingQueue<ServicePlatformMessage> dispatcherQueue, String direction) {
    super(dispatcherQueue);
    this.direction = direction;
  }

  @Override
  public void connectToBus() {
    if (direction.equals("north")) {
      channel = RabbitMqHelperSingleton.getInstance().getNorthChannel();
      consumer = new AdaptorDefaultConsumer(channel, this);
      queueName = RabbitMqHelperSingleton.getInstance().getNorthQueueName();
    } else {
      channel = RabbitMqHelperSingleton.getInstance().getSouthChannel();
      consumer = new AdaptorDefaultConsumer(channel, this);
      queueName = RabbitMqHelperSingleton.getInstance().getSouthQueueName();
    }
  }

  @Override
  public void run() {
    try {
      if (direction.equals("north")) {
        channel = RabbitMqHelperSingleton.getInstance().getNorthChannel();
      } else {
        channel = RabbitMqHelperSingleton.getInstance().getSouthChannel();
      }
      Logger.info("Starting consumer thread");
      channel.basicConsume(queueName, true, consumer);

    } catch (IOException e) {
      Logger.error(e.getMessage(), e);
    }
  }

  @Override
  public boolean startConsuming() {
    boolean out = true;
    Thread thread;
    try {
      thread = new Thread(this);
      thread.start();
    } catch (Exception e) {
      Logger.error(e.getMessage(), e);
      out = false;
    }
    return out;
  }

  @Override
  public boolean stopConsuming() {
    boolean out = true;
    try {
      channel.close();
    } catch (IOException e) {
      Logger.error(e.getMessage(), e);
      out = false;
    } catch (TimeoutException e) {
      Logger.error(e.getMessage(), e);
      out = false;
    }

    return out;
  }


}
