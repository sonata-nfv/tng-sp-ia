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

import org.slf4j.LoggerFactory;
import sonata.kernel.vimadaptor.commons.nsd.VirtualLink;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class ServiceVirtualLinksRepo {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(ServiceVirtualLinksRepo.class);
  private static ServiceVirtualLinksRepo myInstance = null;

  /**
   * Singleton method to get the instance of the ResourceRepo.
   *
   * @return the instance of the ResourceRepo
   */
  public static ServiceVirtualLinksRepo getInstance() {
    if (myInstance == null) {
      myInstance = new ServiceVirtualLinksRepo();
    }
    return myInstance;
  }

  private ConcurrentHashMap<String, ConcurrentHashMap<String, ArrayList<String>>> ServiceVirtualLinksCpMap;
  private ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> ServiceVirtualLinksAccessMap;
  private ConcurrentHashMap<String, ConcurrentHashMap<String, String>> ServiceVirtualLinksNetIdMap;



  private ServiceVirtualLinksRepo() {
    ServiceVirtualLinksCpMap = new ConcurrentHashMap<>();
    ServiceVirtualLinksAccessMap = new ConcurrentHashMap<>();
    ServiceVirtualLinksNetIdMap = new ConcurrentHashMap<>();
  }


  /**
   * Get the status of a specific service id
   *
   * @param serviceId The id of the service
   *
   * @return Boolean
   */
  public Boolean getStatusServiceVirtualLinksFromServiceId(String serviceId) {
    return ServiceVirtualLinksCpMap.containsKey(serviceId);
  }


  /**
   * Return the virtual link id for a specific service id and connection point
   *
   * @param serviceId The id of the service
   * @param connectionPoint The id of the connection point
   *
   * @return String with the virtual link id for a specific service id and connection point
   */
  public String getVirtualLinkIdFromServiceIdAndConnectionPoint(String serviceId, String connectionPoint) {
    if (ServiceVirtualLinksCpMap.containsKey(serviceId)) {
      ConcurrentHashMap<String, ArrayList<String>> virtualLinksMap = ServiceVirtualLinksCpMap.get(serviceId);
      for ( ConcurrentHashMap.Entry<String, ArrayList<String>> virtualLinkEntry : virtualLinksMap.entrySet()) {
        if (virtualLinkEntry.getValue().contains(connectionPoint)) {
          return virtualLinkEntry.getKey();
        }
      }
    }
    return null;
  }

  /**
   * Return the connection points for a specific service id and virtual link id
   *
   * @param serviceId The id of the service
   * @param virtualLinkId The id of the virtual link
   *
   * @return ArrayList with the connections points for a specific service id and virtual link id
   */
  public ArrayList<String> getConnectionPointsFromServiceIdAndVirtualLink(String serviceId, String virtualLinkId) {
    ArrayList<String> connectionPointsForServiceIdAndVirtualLinkId = new ArrayList<>();
    if (ServiceVirtualLinksCpMap.containsKey(serviceId)) {
      ConcurrentHashMap<String, ArrayList<String>> virtualLinksMap = ServiceVirtualLinksCpMap.get(serviceId);
      connectionPointsForServiceIdAndVirtualLinkId = virtualLinksMap.get(virtualLinkId);
    } else {
      connectionPointsForServiceIdAndVirtualLinkId = null;
    }
    return connectionPointsForServiceIdAndVirtualLinkId;
  }


  /**
   * Return the virtual link access for a specific service id and virtual link id
   *
   * @param serviceId The id of the service
   * @param virtualLinkId The id of the virtual link
   *
   * @return Boolean with the virtual link access
   */
  public Boolean getVirtualLinkAccessFromServiceIdAndVirtualLinkId(String serviceId, String virtualLinkId) {
    if (ServiceVirtualLinksAccessMap.containsKey(serviceId)) {
      return ServiceVirtualLinksAccessMap.get(serviceId).get(virtualLinkId);
    }
    return null;
  }


  /**
   * Return the network id for a specific service id and virtual link id
   *
   * @param serviceId The id of the service
   * @param virtualLinkId The id of the virtual link
   *
   * @return String with the network id for a specific service id and virtual link id
   */
  public String getNetworkIdFromServiceIdAndVirtualLinkId(String serviceId, String virtualLinkId) {
    if (ServiceVirtualLinksNetIdMap.containsKey(serviceId)) {
      ConcurrentHashMap<String, String> virtualLinksNetIdMap = ServiceVirtualLinksNetIdMap.get(serviceId);
      for ( ConcurrentHashMap.Entry<String, String> virtualLinkEntry : virtualLinksNetIdMap.entrySet()) {
        if (virtualLinkEntry.getKey().equals(virtualLinkId)) {
          return virtualLinkEntry.getValue();
        }
      }
    }
    return null;
  }


  /**
   * Store the content for specific service id
   *
   * @param serviceId The id of the request
   * @param virtualLinks The array of virtual link to store
   *
   * @return True
   */
  public Boolean putServiceVirtualLinksForServiceId(String serviceId, ArrayList<VirtualLink> virtualLinks) {
    ConcurrentHashMap<String, ArrayList<String>> virtualLinksMap;
    ConcurrentHashMap<String, Boolean> virtualLinksAccessMap;
    ConcurrentHashMap<String, String> virtualLinksNetIdMap;
    if (!ServiceVirtualLinksCpMap.containsKey(serviceId)) {
      virtualLinksMap = new ConcurrentHashMap<>();
      ServiceVirtualLinksCpMap.put(serviceId,virtualLinksMap);
      virtualLinksAccessMap = new ConcurrentHashMap<>();
      ServiceVirtualLinksAccessMap.put(serviceId,virtualLinksAccessMap);
      virtualLinksNetIdMap = new ConcurrentHashMap<>();
      ServiceVirtualLinksNetIdMap.put(serviceId,virtualLinksNetIdMap);
    }

    virtualLinksMap = ServiceVirtualLinksCpMap.get(serviceId);
    virtualLinksAccessMap = ServiceVirtualLinksAccessMap.get(serviceId);
    virtualLinksNetIdMap = ServiceVirtualLinksNetIdMap.get(serviceId);

    for (VirtualLink link : virtualLinks) {
      virtualLinksMap.put(link.getId(),link.getConnectionPointsReference());
      if (link.isAccess() != null) {
        virtualLinksAccessMap.put(link.getId(),link.isAccess());
      }
      if (link.getNetworkId() != null) {
        virtualLinksNetIdMap.put(link.getId(),link.getNetworkId());
      }
    }

    return true;
  }

  /**
   * Remove the map stored for a specific service id
   *
   * @param serviceId The id of the service
   *
   */
  public void removeServiceVirtualLinksFromServiceId(String serviceId) {
    if (ServiceVirtualLinksCpMap.containsKey(serviceId)) {
      ServiceVirtualLinksCpMap.remove(serviceId);
      ServiceVirtualLinksAccessMap.remove(serviceId);
      ServiceVirtualLinksNetIdMap.remove(serviceId);
    }
  }


}
