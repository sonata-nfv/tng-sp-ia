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

package sonata.kernel.vimadaptor.wrapper;

import sonata.kernel.vimadaptor.commons.*;
import sonata.kernel.vimadaptor.commons.nsd.VirtualLink;

import java.io.IOException;
import java.util.ArrayList;

public abstract class ComputeWrapper extends AbstractWrapper implements Wrapper {


  public ComputeWrapper() {
    this.setType(WrapperType.COMPUTE);
  }

  /**
   * general constructor for wrappers of type compute.
   */
  public ComputeWrapper(WrapperConfiguration config) {

    this.setType(WrapperType.COMPUTE);
    this.setConfig(config);
  }

  /**
   * Deploy a the VNF described in the payload in this compute VIM.
   * 
   * @param data the payload of a Function.Deploy call
   * @param sid the session ID for this Adaptor call.
   */
  public abstract void deployFunction(FunctionDeployPayload data, String sid);

  /**
   * Deploy a service instance on this VIM.
   * 
   * @param data the payload containing the service descriptors and the metadata for this service
   *        deployment
   * @param callSid the call processor to notify on completion
   * 
   * @return true if the remove process has started correctly, false otherwise
   */
  @Deprecated
  public abstract boolean deployService(ServiceDeployPayload data, String callSid) throws Exception;

  /**
   * Get the resource utilisation status of this compute VIM.
   * 
   * @return the ResourceUtilisation object representing the status of this VIM
   */
  public abstract ResourceUtilisation getResourceUtilisation();


  /**
   * Check if given image is stored in this compute VIM image repository.
   * 
   * @param image the object representing the VNF image
   */
  public abstract boolean isImageStored(VnfImage image, String callSid);

  /**
   * Prepare a service instance in this VIM for the given instance ID.
   * 
   * @param instanceId the ID of the instance used as reference for the prepared environment in the
   *        VIM
   * @param virtualLinks the virtual links containing information for the network creation
   * 
   * @return true if the remove process has started correctly, false otherwise
   */
  public abstract boolean prepareService(String instanceId, ArrayList<VirtualLink> virtualLinks) throws Exception;

  /**
   * Network create for a service instance in this VIM for the given instance ID.
   *
   * @param instanceId the ID of the instance used as reference for the environment in the
   *        VIM
   * @param virtualLinks the virtual links containing information for the network creation
   *
   * @return true if the remove process has started correctly, false otherwise
   */
  public abstract boolean networkCreate(String instanceId, ArrayList<VirtualLink> virtualLinks) throws Exception;

  /**
   * Network delete for a service instance in this VIM for the given instance ID.
   *
   * @param instanceId the ID of the instance used as reference for the environment in the
   *        VIM
   * @param virtualLinks the virtual links containing information for the network deletion
   *
   * @return true if the remove process has started correctly, false otherwise
   */
  public abstract boolean networkDelete(String instanceId, ArrayList<VirtualLink> virtualLinks) throws Exception;

  /**
   * Remove the given image from this compute VIM image repository.
   * 
   * @param image the object representing the VNF image
   */
  public abstract void removeImage(VnfImage image);

  /**
   * Remove a service instance from this VIM.
   * 
   * @param data the payload of a service.Remove call
   * @param sid the session ID for this Adaptor call
   * 
   */
  public abstract void removeService(ServiceRemovePayload data, String sid);

  /**
   * Scale the VNF described in the payload in this compute VIM
   * 
   * @param data the payload of a Function.Scale call
   * @param sid the session ID for this Adaptor call
   */

  public abstract void scaleFunction(FunctionScalePayload data, String sid);

  /**
   * Remove a VNF described in the payload in this compute VIM
   * 
   * @param data the payload of a Function.Remove call
   * @param sid the session ID for this Adaptor call
   */

  public abstract void removeFunction(FunctionRemovePayload data, String sid);

  /**
   * Upload the given image to this compute VIM image repository.
   * 
   * @param image the VnfImage with URL from which the image can be downloaded.
   */
  public abstract void uploadImage(VnfImage image) throws IOException;

  /**
   * Prepare a slice instance in this VIM for the given instance ID.
   *
   * @param instanceId the ID of the instance used as reference for the prepared environment in the
   *        VIM
   * @param virtualLinks the virtual links containing information for the network creation
   *
   * @return true if the remove process has started correctly, false otherwise
   */
  public abstract boolean prepareSlice(String instanceId, ArrayList<VirtualLink> virtualLinks) throws Exception;

  /**
   * Remove a slice instance from this VIM.
   *
   * @param data the payload of a slice.Remove call
   * @param sid the session ID for this Adaptor call
   *
   */
  public abstract void removeSlice(SliceRemovePayload data, String sid);
}
