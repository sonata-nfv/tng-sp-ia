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

package sonata.kernel.adaptor.wrapper;

import org.slf4j.LoggerFactory;

import sonata.kernel.adaptor.wrapper.mock.WimMockWrapper;

public class WrapperFactory {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(WrapperFactory.class);

  /**
   * Uses the parser configuration to create the relevant Wrapper.
   * 
   * @param config the VimWrapperConfiguration object describing the wrapper to create.
   * @return the brand new wrapper
   */
  public static Wrapper createWrapper(VimWrapperConfiguration config) {
    Wrapper output = null;
    Logger.debug("Factory - Creating wrapper...");
    if (config.getWrapperType().equals(WrapperType.COMPUTE)) {
      Logger.debug("Factory - Creating Compute Wrapper.");
      output = createComputeWrapper(config);
    }
    if (config.getWrapperType().equals(WrapperType.NETWORK)) {
      Logger.debug("Factory - Creating Network Wrapper.");
      output = createNetworkWrapper(config);
    }
    if (config.getWrapperType().equals(WrapperType.STORAGE)) {
      Logger.debug("Factory - Creating Storage Wrapper.");
      output = createStorageWrapper(config);
    }
    if (output != null) {
      Logger.debug("Factory - Wrapper created.");
    } else {
      Logger.debug("Factory - Unable to create wrapper.");

    }
    return output;
  }

  /**
   * Uses the parser configuration to create the relevant Wim Wrapper.
   *
   * @param config the VimWrapperConfiguration object describing the wrapper to create.
   * @return the brand new wrapper
   */
  public static Wrapper createWimWrapper(WimWrapperConfiguration config) {
    Wrapper output = null;
    Logger.info("  [WrapperFactory] - creating wrapper...");

    if (config.getWimVendor().equals(WimVendor.MOCK)){
      output = new WimMockWrapper(config);
    }

    Logger.info("  [WrapperFactory] - Wrapper created...");
    return output;
  }

  private static ComputeWrapper createComputeWrapper(VimWrapperConfiguration config) {
    return null;
  }

  private static NetworkWrapper createNetworkWrapper(VimWrapperConfiguration config) {return null;}

  private static StorageWrapper createStorageWrapper(VimWrapperConfiguration config) {
    return null;
  }
}
