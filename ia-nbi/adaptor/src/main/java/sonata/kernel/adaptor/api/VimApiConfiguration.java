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

package sonata.kernel.adaptor.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VimApiConfiguration {

  private String uuid;
  private String name;
  private String country;
  private String city;
  private String endpoint;
  @JsonProperty("username")
  private String userName;
  private String password;
  private String authKey;
  private String tenant;
  private String domain;
  @JsonProperty("network_endpoint")
  private String networkEndpoint;
  @JsonProperty("private_network_prefix")
  private String privateNetworkPrefix;
  @JsonProperty("private_network_length")
  private String privateNetworkLength;
  @JsonProperty("external_network_id")
  private String externalNetworkId;
  @JsonProperty("external_router_id")
  private String externalRouterId;



  public String getUuid() {
    return this.uuid;
  }

  public String getName() {
    return name;
  }

  public String getCountry() {
    return country;
  }

  public String getCity() {
    return city;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getUserName() {
    return userName;
  }

  public String getPassword() {
    return password;
  }

  public String getAuthKey() {
    return authKey;
  }

  public String getTenant() {
    return tenant;
  }

  public String getDomain() {
    return domain;
  }

  public String getNetworkEndpoint() {
    return networkEndpoint;
  }

  public String getPrivateNetworkPrefix() {
    return privateNetworkPrefix;
  }

  public String getPrivateNetworkLength() {
    return privateNetworkLength;
  }

  public String getExternalNetworkId() {
    return externalNetworkId;
  }

  public String getExternalRouterId() {
    return externalRouterId;
  }


  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setAuthKey(String authKey) {
    this.authKey = authKey;
  }

  public void setTenant(String tenant) {
    this.tenant = tenant;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public void setNetworkEndpoint(String networkEndpoint) {
    this.networkEndpoint = networkEndpoint;
  }

  public void setPrivateNetworkPrefix(String privateNetworkPrefix) {
    this.privateNetworkPrefix = privateNetworkPrefix;
  }

  public void setPrivateNetworkLength(String privateNetworkLength) {
    this.privateNetworkLength = privateNetworkLength;
  }

  public void setExternalNetworkId(String externalNetworkId) {
    this.externalNetworkId = externalNetworkId;
  }

  public void setExternalRouterId(String externalRouterId) {
    this.externalRouterId = externalRouterId;
  }

}
