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
import java.util.concurrent.ConcurrentHashMap;


public class ResourceRepo {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ResourceRepo.class);
  private static ResourceRepo myInstance = null;

  /**
   * Singleton method to get the instance of the ResourceRepo.
   *
   * @return the instance of the ResourceRepo
   */
  public static ResourceRepo getInstance() {
    if (myInstance == null) {
      myInstance = new ResourceRepo();
    }
    return myInstance;
  }

  private ConcurrentHashMap<String, ConcurrentHashMap<VimVendor, String>> ResourceRepoMap;
  private ConcurrentHashMap<String, Integer> ResourceSizeMap;


  private ResourceRepo() {
    ResourceRepoMap = new ConcurrentHashMap<>();
    ResourceSizeMap = new ConcurrentHashMap<>();
  }



  /**
   * Return the number of vendors stored for a specific request id
   *
   * @param requestId The id of the request
   *
   * @return an integer with the number of vendors stored for a specific request id
   */
  public Integer getStoredVendorsNumberForRequestId(String requestId) {
    if (ResourceRepoMap.containsKey(requestId)) {
      return ResourceRepoMap.get(requestId).size();
    } else {
      return 0;
    }

  }

  /**
   * Return the number of vendors expected for a specific request id
   *
   * @param requestId The id of the request
   *
   * @return an integer with the number of vendors expected for a specific request id
   */
  public Integer getExpectedVendorsNumberForRequestId(String requestId) {
    if (ResourceRepoMap.containsKey(requestId)) {
      return ResourceSizeMap.get(requestId);
    } else {
      return -1;
    }

  }

  /**
   * Get the status of a specific request id
   *
   * @param requestId The id of the request
   *
   * @return Boolean
   */
  public Boolean getStatusResourcesFromRequestId(String requestId) {
    if (ResourceRepoMap.containsKey(requestId)) {
      return true;
    }
    return false;
  }

  /**
   * Return the content stored for a specific request id as a array list
   *
   * @param requestId The id of the request
   *
   * @return ArrayList with the content for a specific request id
   */
  public ArrayList<String> getResourcesFromRequestId(String requestId) {
    ArrayList<String> resourceForRequestId = new ArrayList<>();
    if (ResourceRepoMap.containsKey(requestId)) {
      ConcurrentHashMap<VimVendor, String> resourceMap = ResourceRepoMap.get(requestId);
      resourceForRequestId.addAll(resourceMap.values());
    } else {
      resourceForRequestId = null;
    }
    return resourceForRequestId;
  }

  /**
   * Store the specific request id
   *
   * @param requestId The id of the request
   *
   * @return Boolean
   */
  public Boolean putResourcesForRequestId(String requestId, int vendorSize) {
    ConcurrentHashMap<VimVendor, String> resourceMap;
    if (ResourceRepoMap.containsKey(requestId)) {
      return false;
    }

    resourceMap = new ConcurrentHashMap<>();
    ResourceRepoMap.put(requestId,resourceMap);

    ResourceSizeMap.put(requestId,vendorSize);

    return true;
  }

  /**
   * Store the content from vendor for specific request id
   *
   * @param requestId The id of the request
   * @param vendor The vendor name
   * @param content The resource content
   *
   * @return True
   */
  public Boolean putResourcesForRequestIdAndVendor(String requestId, VimVendor vendor, String content) {
    ConcurrentHashMap<VimVendor, String> resourceMap;
    if (!ResourceRepoMap.containsKey(requestId)) {
      return false;
    }

    resourceMap = ResourceRepoMap.get(requestId);
    resourceMap.put(vendor,content);
    return true;
  }

  /**
   * Remove the map stored for a specific request id
   *
   * @param requestId The id of the request
   *
   */
  public void removeResourcesFromRequestId(String requestId) {
    if (ResourceRepoMap.containsKey(requestId)) {
      ResourceRepoMap.remove(requestId);
      ResourceSizeMap.remove(requestId);
    }
  }


}
