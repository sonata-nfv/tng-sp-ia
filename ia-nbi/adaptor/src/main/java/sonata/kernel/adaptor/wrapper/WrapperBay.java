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


import java.util.ArrayList;

public class WrapperBay {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(WrapperBay.class);
  private static WrapperBay myInstance = null;

  /**
   * Singleton method to get the instance of the wrapperbay.
   * 
   * @return the instance of the wrapperbay
   */
  public static WrapperBay getInstance() {
    if (myInstance == null) {
      myInstance = new WrapperBay();
    }
    return myInstance;
  }


  private VimRepo vimRepository = null;
  private WimRepo wimRepository = null;

  private WrapperBay() {
  }


  /**
   * Utility methods to clear registry tables.
   */
  public void clear() {}



  /**
   * Return the wrapper of the compute VIM identified by the given UUID.
   * 
   * @param vimUuid the UUID of the compute VIM
   * 
   * @return the wrapper of the requested VIM or null if the UUID does not correspond to a
   *         registered VIM
   */
  public VimWrapperConfiguration getComputeConfig(String vimUuid) {
    VimWrapperConfiguration vimEntry = this.vimRepository.readVimEntry(vimUuid);
    if (vimEntry == null) {
      return null;
    } else {
      return vimEntry;
    }
  }

  /**
   * Return the list of the registered compute VIMs.
   * 
   * @return an arraylist of String representing the UUIDs of the registered VIMs
   */
  public ArrayList<String> getComputeWrapperList() {
    return vimRepository.getComputeVims();

  }

  /**
   * @return
   */
  public ArrayList<String> getNetworkWrapperList() {
    return vimRepository.getNetworkVims();
  }

  /**
   * Return the VimRepo
   * 
   * @return the VimRepo object.
   */
  public VimRepo getVimRepo() {
    return vimRepository;
  }

  /**
   * Return the WimRepo
   *
   * @return the WimRepo object.
   */
  public WimRepo getWimRepo() {
    return wimRepository;
  }

  /**
   * Return a generic Vim Wrapper for the given Vim UUID
   * 
   * @param uuid
   * @return
   */
  public VimWrapperConfiguration getConfig(String uuid) {
    return this.vimRepository.readVimEntry(uuid);
  }


  /**
   * Register a new compute wrapper.
   * 
   * @param config The configuration object representing the Wrapper to register
   * @return a JSON representing the output of the API call
   */
  public String registerComputeWrapper(VimWrapperConfiguration config) {

    this.vimRepository.writeVimEntry(config.getUuid(), config);
    String output = "{\"request_status\":\"COMPLETED\",\"uuid\":\"" + config.getUuid() + "\"}";


    return output;
  }

  /**
   * Registre a new Network VIM to the wrapper bay.
   * 
   * @param config
   * @param computeVimRef
   * @return a JSON formatte string with the result of the registration.
   */
  public String registerNetworkWrapper(VimWrapperConfiguration config, String computeVimRef) {

    this.vimRepository.writeVimEntry(config.getUuid(), config);
    this.vimRepository.writeNetworkVimLink(computeVimRef, config.getUuid());
    String output = "{\"request_status\":\"COMPLETED\",\"uuid\":\"" + config.getUuid() + "\"}";

    return output;
  }

  /**
   * Remove a registered compute wrapper from the IA.
   * 
   * @param uuid the uuid of the wrapper to remove
   * @return a JSON representing the output of the API call
   */
  public String removeComputeWrapper(String uuid) {
    vimRepository.removeVimEntry(uuid);
    return "{\"request_status\":\"COMPLETED\"}";
  }

  /**
   * @param uuid
   * @return
   */
  public String removeNetworkWrapper(String uuid) {
    this.vimRepository.removeNetworkVimLink(uuid);
    this.vimRepository.removeVimEntry(uuid);
    return "{\"request_status\":\"COMPLETED\"}";
  }

  /**
   * Set the Database reader/writer to use as a vimRepository for VIMs.
   * 
   * @param repo the Database reader/writer to store the wrappers
   */
  public void setVimRepo(VimRepo repo) {
    this.vimRepository = repo;
  }


  /**
   * Set the Database reader/writer to use as a vimRepository for VIMs.
   *
   * @param repo the Database reader/writer to store the wrappers
   */
  public void setWimRepo(WimRepo repo) {
    this.wimRepository = repo;
  }

  /**
   * Register a new WIM wrapper to the WIM adaptor.
   *
   * @param config the VimWrapperConfiguration for the WIM wrapper to be created
   * @return a JSON formatted string with the result of the operation
   */
  public String registerWimWrapper(WimWrapperConfiguration config) {

    this.wimRepository.writeWimEntry(config.getUuid(), config);
    String output = "{\"request_status\":\"COMPLETED\",\"uuid\":\"" + config.getUuid()
              + "\",\"message\":\"\"}";

    return output;
  }

  public WimWrapperConfiguration getWimConfigFromWimUuid(String wimUuid) {
    WimWrapperConfiguration out;
    out = this.wimRepository.readWimEntry(wimUuid);
    return out;
  }

  public String removeWimWrapper(String uuid) {
    wimRepository.removeWimVimLink(uuid);
    wimRepository.removeWimEntry(uuid);
    return "{\"request_status\":\"COMPLETED\"}";
  }

  public ArrayList<String> getWimList() {
    return wimRepository.listWims();
  }

  public String attachVim(String wimUuid, String vimUuid, String vimAddress) {
    boolean result = wimRepository.attachVim(wimUuid, vimUuid, vimAddress);
    if(result)
      return "{\"request_status\":\"COMPLETED\"}";
    else
      return "{\"request_status\":\"ERROR\",\"message\":\"Unable to write VIM attachment into WIM vimRepository\"}";
  }

  public ArrayList<String> getAttachedVims(String wimUuid) {
    return wimRepository.readAttachedVim(wimUuid);
  }

  public String getVimAddressFromVimUuid(String vimUuid){
    return wimRepository.readVimAddressFromVimUuid(vimUuid);
  }

}
