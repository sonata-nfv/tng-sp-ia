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
 * @author Carlos Marques (ALB)
 * 
 */

package sonata.kernel.adaptor.commons;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

//import sonata.kernel.adaptor.commons.ServicePreparePayload;
//import sonata.kernel.adaptor.commons.SonataManifestMapper;
//import sonata.kernel.adaptor.commons.VimPreDeploymentList;
import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.wrapper.VimRepo;
import sonata.kernel.adaptor.wrapper.ComputeVimVendor;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;


public class GetVimVendors {
	private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(GetVimVendors.class);
	private Properties prop;

	/**
	 * Create the a GetVimVendors that read from the config file.
	 * 
	 */
	public GetVimVendors() {
		this.prop = VimRepo.parseConfigFile();
	}
	
	
	/**
	 * Retrieve the vim Vendors from the info in the message received.
	 * 
	 * @param message the message received from RebbitMQ
	 * 
	 * @param topic the action present in topic
	 *
	 * @return the list of vim Vendors or null 
	 */
	public ArrayList<String> GetVimVendors(ServicePlatformMessage message, String topic) {
		if (topic.equals("prepare")) {
            return this.GetVimVendorsPrepare(message);
        } else if (topic.equals("deploy")) {
            return this.GetVimVendorsDeploy(message);
        } else if (topic.equals("function.remove")) {
            return this.GetVimVendorsFRemove(message);
        } else if (topic.equals("service.remove")) {
            return this.GetVimVendorsSRemove(message);
        } else if (topic.equals("scale")) {
            return this.GetVimVendorsScale(message);
        } else if (topic.equals("chain.configure")) {
            return this.GetVimVendorsCConfigure(message);
        } else if (topic.equals("chain.deconfigure")) {
            return this.GetVimVendorsCDeconfigure(message);
        } else if (topic.equals("compute.list")) {
            return this.GetVimVendorsCList(message);
        } else {
			return null;
		}

	}
  
	/**
	 * Retrieve the vim Vendors from the info in the message received for service prepare.
	 * 
	 * @param message the message received from RabbitMQ
	 *
	 * @return the list of vim Vendors or null 
	 */
	private ArrayList<String> GetVimVendorsPrepare(ServicePlatformMessage message) {
		  
		Logger.info("Call received - sid: " + message.getSid());
		// parse the payload to get VIM UUID from the request body
		Logger.info("Parsing payload...");
		ServicePreparePayload payload = null;
		ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
		ArrayList<String> vimUuids = new ArrayList<String>();
		ArrayList<String> vimVendors = null;

		try {
			payload = mapper.readValue(message.getBody(), ServicePreparePayload.class);
			Logger.info("payload parsed. Get VIMs");

	        for (VimPreDeploymentList vim : payload.getVimList()) {
                vimUuids.add(vim.getUuid());
            }
			if (vimUuids.isEmpty()) {
				Logger.error("Error retrieving the Vims uuid");
				
				return null;
			}
			Logger.info(message.getSid().substring(0, 10) + " - Vims retrieved");

			// Get the types from db
			vimVendors = this.GetVimVendorsDB(vimUuids);

		} catch (Exception e) {
			Logger.error("Error retrieving the Vims Type: " + e.getMessage(), e);

			return null;
		}
		return vimVendors;
	}

    /**
     * Retrieve the vim Vendors from the info in the message received for service deploy.
     *
     * @param message the message received from RabbitMQ
     *
     * @return the list of vim Vendors or null
     */
    private ArrayList<String> GetVimVendorsDeploy(ServicePlatformMessage message) {

        Logger.info("Call received - sid: " + message.getSid());
        // parse the payload to get VIM UUID from the request body
        Logger.info("Parsing payload...");
        FunctionDeployPayload data = null;
        ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
        ArrayList<String> vimUuids = new ArrayList<String>();
        ArrayList<String> vimVendors = null;

        try {
            data = mapper.readValue(message.getBody(), FunctionDeployPayload.class);
            Logger.info("payload parsed");
            vimUuids.add(data.getVimUuid());

            if (vimUuids.isEmpty()) {
                Logger.error("Error retrieving the Vims uuid");

                return null;
            }

            Logger.info(message.getSid().substring(0, 10) + " - Vims retrieved");

            // Get the types from db
            vimVendors = this.GetVimVendorsDB(vimUuids);

        } catch (Exception e) {
            Logger.error("Error retrieving the Vims Type: " + e.getMessage(), e);

            return null;
        }

        return vimVendors;
    }

    /**
     * Retrieve the vim Vendors from the info in the message received for function remove.
     *
     * @param message the message received from RabbitMQ
     *
     * @return the list of vim Vendors or null
     */
    private ArrayList<String> GetVimVendorsFRemove(ServicePlatformMessage message) {

        Logger.info("Call received - sid: " + message.getSid());
        // process json message to get the service UUID from the request body
        Logger.info("Process payload...");
        FunctionRemovePayload data = null;
        ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
        ArrayList<String> vimUuids = new ArrayList<String>();
        ArrayList<String> vimVendors = null;

        try {
            data = mapper.readValue(message.getBody(), FunctionRemovePayload.class);
            Logger.info("payload parsed");
            vimUuids.add(data.getVimUuid());

            if (vimUuids.isEmpty()) {
                Logger.error("Error retrieving the Vims uuid");

                return null;
            }
            Logger.info(message.getSid().substring(0, 10) + " - Vims retrieved");

            // Get the types from db
            vimVendors = this.GetVimVendorsDB(vimUuids);

        } catch (Exception e) {
            Logger.error("Error retrieving the Vims Type: " + e.getMessage(), e);

            return null;
        }

        return vimVendors;
    }


    /**
     * Retrieve the vim Vendors from the info in the message received for service remove.
     *
     * @param message the message received from RabbitMQ
     *
     * @return the list of vim Vendors or null
     */
    private ArrayList<String> GetVimVendorsSRemove(ServicePlatformMessage message) {

        Logger.info("Call received - sid: " + message.getSid());
        // process json message to get the service UUID from the request body
        Logger.info("Process payload...");
        JSONTokener tokener = new JSONTokener(message.getBody());
        JSONObject jsonObject = (JSONObject) tokener.nextValue();
        String instanceUuid = null;
        try {
            instanceUuid = jsonObject.getString("instance_uuid");
        } catch (Exception e) {
            Logger.error("Error getting the instance_uuid: " + e.getMessage(), e);

            return null;
        }

        ArrayList<String> vimUuids = null;
        ArrayList<String> vimVendors = null;

        try {
            vimUuids = getComputeVimUuidFromInstance(instanceUuid);

            if (vimUuids == null) {
                Logger.error("Error retrieving the Vims uuid");

                return null;
            }
            Logger.info(message.getSid().substring(0, 10) + " - Vims retrieved");

            // Get the types from db
            vimVendors = this.GetVimVendorsDB(vimUuids);

        } catch (Exception e) {
            Logger.error("Error retrieving the Vims Type: " + e.getMessage(), e);

            return null;
        }

        return vimVendors;
    }

    /**
     * Retrieve the vim Vendors from the info in the message received for service scale.
     *
     * @param message the message received from RabbitMQ
     *
     * @return the list of vim Vendors or null
     */
    private ArrayList<String> GetVimVendorsScale(ServicePlatformMessage message) {

        Logger.info("Call received - sid: " + message.getSid());
        // parse the payload to get VIM UUID from the request body
        Logger.info("Parsing payload...");
        FunctionScalePayload data = null;
        ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
        ArrayList<String> vimUuids = new ArrayList<String>();
        ArrayList<String> vimVendors = null;

        try {
            data = mapper.readValue(message.getBody(), FunctionScalePayload.class);
            Logger.info("payload parsed");
            vimUuids.add(data.getFunctionInstanceId());

            if (vimUuids.isEmpty()) {
                Logger.error("Error retrieving the Vims uuid");

                return null;
            }
            Logger.info(message.getSid().substring(0, 10) + " - Vims retrieved");

            // Get the types from db
            vimVendors = this.GetVimVendorsDB(vimUuids);

        } catch (Exception e) {
            Logger.error("Error retrieving the Vims Type: " + e.getMessage(), e);

            return null;
        }

        return vimVendors;
    }

    /**
     * Retrieve the vim Vendors from the info in the message received for service chain configure.
     *
     * @param message the message received from RabbitMQ
     *
     * @return the list of vim Vendors or null
     */
    private ArrayList<String> GetVimVendorsCConfigure(ServicePlatformMessage message) {

        Logger.info("Call received - sid: " + message.getSid());
        // process json message to get the service UUID from the request body
        Logger.info("Process payload...");
        NetworkConfigurePayload data = null;
        ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
        ArrayList<String> vimUuids = null;
        ArrayList<String> vimVendors = null;

        try {
            data = mapper.readValue(message.getBody(), NetworkConfigurePayload.class);
            Logger.info("payload parsed");
            String serviceInstaceId = data.getServiceInstanceId();
            vimUuids = getNetworkVimUuidFromInstance(serviceInstaceId);

            if (vimUuids == null) {
                Logger.error("Error retrieving the Vims uuid");

                return null;
            }
            Logger.info(message.getSid().substring(0, 10) + " - Vims retrieved");

            // Get the types from db
            vimVendors = this.GetVimVendorsDB(vimUuids);

        } catch (Exception e) {
            Logger.error("Error retrieving the Vims Type: " + e.getMessage(), e);

            return null;
        }

        return vimVendors;
    }

    /**
     * Retrieve the vim Vendors from the info in the message received for service chain deconfigure.
     *
     * @param message the message received from RabbitMQ
     *
     * @return the list of vim Vendors or null
     */
    private ArrayList<String> GetVimVendorsCDeconfigure(ServicePlatformMessage message) {

        Logger.info("Call received - sid: " + message.getSid());
        // process json message to get the service UUID from the request body
        Logger.info("Process payload...");
        NetworkDeconfigurePayload data = null;
        ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
        ArrayList<String> vimUuids = null;
        ArrayList<String> vimVendors = null;

        try {
            data = mapper.readValue(message.getBody(), NetworkDeconfigurePayload.class);
            Logger.info("payload parsed");
            String serviceInstaceId = data.getServiceInstanceId();
            vimUuids = getNetworkVimUuidFromInstance(serviceInstaceId);

            if (vimUuids == null) {
                Logger.error("Error retrieving the Vims uuid");

                return null;
            }
            Logger.info(message.getSid().substring(0, 10) + " - Vims retrieved");

            // Get the types from db
            vimVendors = this.GetVimVendorsDB(vimUuids);

        } catch (Exception e) {
            Logger.error("Error retrieving the Vims Type: " + e.getMessage(), e);

            return null;
        }

        return vimVendors;
    }

    /**
     * Retrieve the vim Vendors that exist.
     *
     * @param message the message received from RabbitMQ
     *
     * @return the list of vim Vendors or null
     */
    private ArrayList<String> GetVimVendorsCList(ServicePlatformMessage message) {
        ArrayList<String> vimVendors = null;

        vimVendors = ComputeVimVendor.getPossibleVendors();

        if (vimVendors.isEmpty()) {
            return null;
        }

        return vimVendors;
    }

	/**
	 * Retrieve the vim Vendors from the vim uuids consulting the DB.
	 * 
	 * @param vimUuids the vim uuids
	 *
	 * @return the list of vim Vendors or null 
	 */
	private ArrayList<String> GetVimVendorsDB(ArrayList<String> vimUuids) {
		  
		ArrayList<String> vimVendors = new ArrayList<String>();
		  
		
		Connection connection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			Class.forName("org.postgresql.Driver");
			connection =
				  DriverManager.getConnection(
					  "jdbc:postgresql://" + this.prop.getProperty("repo_host") + ":"
						  + this.prop.getProperty("repo_port") + "/" + "vimregistry",
					  this.prop.getProperty("user"), this.prop.getProperty("pass"));
			connection.setAutoCommit(false);

			for (String vimUuid : vimUuids) {
			
				stmt = connection.prepareStatement("SELECT * FROM VIM WHERE UUID=?;");
				stmt.setString(1, vimUuid);
				rs = stmt.executeQuery();

				if (rs.next()) {
					String vendorString = rs.getString("VENDOR").toLowerCase();
					if (!vimVendors.contains(vendorString)) {
						vimVendors.add(vendorString);						
					}

				}
			}
            if (vimVendors.isEmpty()) {
                Logger.error("Error retrieving the Vims Vendor");
                vimVendors = null;
            }
		} catch (SQLException e) {
			Logger.error(e.getMessage());
			vimVendors = null;
		} catch (ClassNotFoundException e) {
			Logger.error(e.getMessage(), e);
			vimVendors = null;
		} finally {
			try {
				if (stmt != null) {
					stmt.close();
				}
				if (rs != null) {
					rs.close();
				}
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				Logger.error(e.getMessage());
				vimVendors = null;
			}
		}
		  
		return vimVendors;
	}

    /**
     * Return a list of the compute VIMs hosting at least one VNFs of the given Service Instance.
     *
     * @param instanceUuid the UUID that identifies the Service Instance
     * @return an array of String objecst representing the UUID of the Compute VIMs
     */
    private ArrayList<String> getComputeVimUuidFromInstance(String instanceUuid) {

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<String> uuids = new ArrayList<String>();

        try {
            Class.forName("org.postgresql.Driver");
            connection =
                    DriverManager.getConnection(
                            "jdbc:postgresql://" + prop.getProperty("repo_host") + ":"
                                    + prop.getProperty("repo_port") + "/" + "vimregistry",
                            prop.getProperty("user"), prop.getProperty("pass"));
            connection.setAutoCommit(false);

            stmt = connection
                    .prepareStatement("SELECT VIM_UUID FROM service_instances  WHERE INSTANCE_UUID=?;");
            stmt.setString(1, instanceUuid);
            rs = stmt.executeQuery();

            while (rs.next()) {
                uuids.add(rs.getString("VIM_UUID"));
            }
            if (uuids.isEmpty()) {
                Logger.error("Error retrieving the Cumpute Vims Uuids");
                uuids = null;
            }
        } catch (SQLException e) {
            Logger.error(e.getMessage());
            uuids = null;
        } catch (ClassNotFoundException e) {
            Logger.error(e.getMessage(), e);
            uuids = null;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                Logger.error(e.getMessage());
                uuids = null;

            }
        }

        return uuids;

    }

    /**
     * Return a list of the network VIMs hosting at least one VNFs of the given Service Instance.
     *
     * @param instanceUuid the UUID that identifies the Service Instance
     * @return an array of String objecst representing the UUID of the Network VIMs
     */
    public ArrayList<String> getNetworkVimUuidFromInstance(String instanceUuid) {

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<String> computeVimUuids = null;
        ArrayList<String> networkVimUuids = new ArrayList<String>();;
        try {
            computeVimUuids = getComputeVimUuidFromInstance(instanceUuid);

            if (computeVimUuids == null) {
                Logger.error("Error retrieving the Compute Vims uuid");

                return null;
            }

        } catch (Exception e) {
            Logger.error("Error retrieving the Cumpute Vims Uuid: " + e.getMessage(), e);

            return null;
        }

        try {
            Class.forName("org.postgresql.Driver");
            connection =
                    DriverManager.getConnection(
                            "jdbc:postgresql://" + prop.getProperty("repo_host") + ":"
                                    + prop.getProperty("repo_port") + "/" + "vimregistry",
                            prop.getProperty("user"), prop.getProperty("pass"));
            connection.setAutoCommit(false);

            for (String computeVimUuid : computeVimUuids) {
                stmt = connection.prepareStatement(
                        "SELECT vim.UUID FROM vim,link_vim WHERE vim.UUID=LINK_VIM.NETWORKING_UUID AND LINK_VIM.COMPUTE_UUID=?;");
                stmt.setString(1, computeVimUuid);
                rs = stmt.executeQuery();

                if (rs.next()) {
                    networkVimUuids.add(rs.getString("UUID"));

                }
            }

            if (networkVimUuids.isEmpty()) {
                Logger.error("Error retrieving the Network Vim Uuids");
                networkVimUuids = null;
            }

        } catch (SQLException e) {
            Logger.error(e.getMessage());
            networkVimUuids = null;
        } catch (ClassNotFoundException e) {
            Logger.error(e.getMessage(), e);
            networkVimUuids = null;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                Logger.error(e.getMessage());
                networkVimUuids = null;

            }
        }

        return networkVimUuids;

    }


}