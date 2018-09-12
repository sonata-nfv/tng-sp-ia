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


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import sonata.kernel.adaptor.wrapper.*;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Unit test for simple App.
 */
public class VimRepoTest {

  private VimRepo repoInstance;

  
  @Before
  public void setUp(){
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

    System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "false");

    System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "warn");

    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "warn");
  }
  
  /**
   * Register, send 4 heartbeat, deregister.
   * 
   * @throws IOException
   */
  @Test
  public void testCreateVimRepo() {

    repoInstance = new VimRepo();
    ArrayList<String> vims = repoInstance.getComputeVims();
    Assert.assertNotNull("Unable to retrieve an empy list. SQL exception occurred", vims);
  }

  @Test
  public void testAddInstance() {

    repoInstance = new VimRepo();

    boolean out = repoInstance.writeServiceInstanceEntry("1", "1-1", "stack1-1",
        "xxxx-xxxxxxxx-xxxxxxxx-xxxx");

    Assert.assertTrue("Errors while writing the instance", out);

    out = repoInstance.removeServiceInstanceEntry("1", "xxxx-xxxxxxxx-xxxxxxxx-xxxx");

    Assert.assertTrue("Errors while removing the instance", out);

  }

  @Test
  public void testGetInstanceVimUuid() {

    repoInstance = new VimRepo();

    boolean out = repoInstance.writeServiceInstanceEntry("1", "1-1", "stack1-1",
        "xxxx-xxxxxxxx-xxxxxxxx-xxxx");

    Assert.assertTrue("Errors while writing the instance", out);

    String vimUuid = repoInstance.getServiceInstanceVimUuid("1");

    Assert.assertTrue("Retrieved vim UUID different from the stored UUID", vimUuid.equals("1-1"));

    out = repoInstance.removeServiceInstanceEntry("1", "xxxx-xxxxxxxx-xxxxxxxx-xxxx");

    Assert.assertTrue("Errors while removing the instance", out);
  }

  @Test
  public void testGetInstanceVimName() {

    repoInstance = new VimRepo();

    boolean out = repoInstance.writeServiceInstanceEntry("1", "1-1", "stack1-1",
        "xxxx-xxxxxxxx-xxxxxxxx-xxxx");

    Assert.assertTrue("Errors while writing the instance", out);

    String vimName = repoInstance.getServiceInstanceVimName("1");

    Assert.assertTrue("Retrieved vim Name different from the stored Name",
        vimName.equals("stack1-1"));

    out = repoInstance.removeServiceInstanceEntry("1", "xxxx-xxxxxxxx-xxxxxxxx-xxxx");

    Assert.assertTrue("Errors while removing the instance", out);

  }
}
