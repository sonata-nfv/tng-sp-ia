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

import sonata.kernel.adaptor.messaging.ServicePlatformMessage;
import sonata.kernel.adaptor.wrapper.WimRepo;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Collections;


public class GetWimVendors {
	private static final org.slf4j.Logger Logger =
      LoggerFactory.getLogger(GetWimVendors.class);
	private Properties prop;

	/**
	 * Create the a GetVimVendors that read from the config file.
	 * 
	 */
	public GetWimVendors() {
		this.prop = WimRepo.parseConfigFile();
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
	public ArrayList<String> GetWimVendors(ServicePlatformMessage message, String topic) {
		if (topic.equals("configure")) {
            return this.GetWimVendorsConfigure(message);
        } else if (topic.equals("deconfigure")) {
            return this.GetWimVendorsDeconfigure(message);
		} else {
			return null;
		}
		  
	}
  
	/**
	 * Retrieve the wim Vendors from the info in the message received for service wan configure.
	 * 
	 * @param message the message received from RabbitMQ
	 *
	 * @return the list of vim Vendors or null 
	 */
	private ArrayList<String> GetWimVendorsConfigure(ServicePlatformMessage message) {

        Logger.info("Call received - sid: " + message.getSid());
        // parse the payload to get ??? from the request body
        Logger.info("Parsing payload...");
        ConfigureWanPayload request = null;
        ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
        ArrayList<String> wimUuids = new ArrayList<String>();
        ArrayList<String> wimVendors = null;

		try {
            request = mapper.readValue(message.getBody(), ConfigureWanPayload.class);
			Logger.info("payload parsed. Get VIMs list");

            ArrayList<ComparableUuid> vims = request.getVimList();
            Collections.sort(vims);
            ArrayList<String> vimsUuid = new ArrayList<String>(vims.size());
            for (ComparableUuid uuid : vims)
                vimsUuid.add(uuid.getUuid());

            for (String vimUuid : vimsUuid) {
                ArrayList<String> wimUuidsToVim = readWimEntryFromVimUuid(vimUuid);
                if (wimUuidsToVim == null) {
                    Logger.error("Error in wan configuration call: Can't find the WIM to wich VIM " + vimUuid
                            + " is attached");

                    return wimVendors;
                }
                for (String wimUuidToVim : wimUuidsToVim) {
                    if (!wimUuids.contains(wimUuidToVim)) {
                        wimUuids.add(wimUuidToVim);
                    }
                }
            }

			if (wimUuids.isEmpty()) {
				Logger.error("Error retrieving the Wims uuid");
				
				return wimVendors;
			}
			Logger.info(message.getSid().substring(0, 10) + " - Wims retrieved");

			// Get the types from db
			wimVendors = this.GetWimVendorsDB(wimUuids);

		} catch (Exception e) {
			Logger.error("Error retrieving the Wims Type: " + e.getMessage(), e);

			return null;
		}
		return wimVendors;
	}

    /**
     * Retrieve the wim Vendors from the info in the message received for service wan deconfigure.
     *
     * @param message the message received from RabbitMQ
     *
     * @return the list of vim Vendors or null
     */
    private ArrayList<String> GetWimVendorsDeconfigure(ServicePlatformMessage message) {

        Logger.info("Call received - sid: " + message.getSid());
        // parse the payload to get ??? from the request body
        Logger.info("Parsing payload...");
        DeconfigureWanPayload request = null;
        ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
        ArrayList<String> wimUuids = new ArrayList<String>();
        ArrayList<String> wimVendors = null;

        try {
            request = mapper.readValue(message.getBody(), DeconfigureWanPayload.class);
            Logger.info("payload parsed. Get Service Instance Id");
            String instanceId = request.getServiceInstanceId();

            wimUuids = getWimUuidFromInstance(instanceId);

            if (wimUuids == null) {
                Logger.error("Error retrieving the Wims uuid");

                return wimVendors;
            }
            Logger.info(message.getSid().substring(0, 10) + " - Wims retrieved");

            // Get the types from db
            wimVendors = this.GetWimVendorsDB(wimUuids);

        } catch (Exception e) {
            Logger.error("Error retrieving the Wims Type: " + e.getMessage(), e);

            return null;
        }
        return wimVendors;

    }


	/**
	 * Retrieve the wim Vendors from the wim uuids consulting the DB.
	 * 
	 * @param wimUuids the vim uuids
	 *
	 * @return the list of wim Vendors or null
	 */
	private ArrayList<String> GetWimVendorsDB(ArrayList<String> wimUuids) {
		  
		ArrayList<String> wimVendors = new ArrayList<String>();
		  
		
		Connection connection = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			Class.forName("org.postgresql.Driver");
			connection =
				  DriverManager.getConnection(
					  "jdbc:postgresql://" + this.prop.getProperty("repo_host") + ":"
						  + this.prop.getProperty("repo_port") + "/" + "wimregistry",
					  this.prop.getProperty("user"), this.prop.getProperty("pass"));
			connection.setAutoCommit(false);

			for (String wimUuid : wimUuids) {
			
				stmt = connection.prepareStatement("SELECT * FROM WIM WHERE UUID=?;");
				stmt.setString(1, wimUuid);
				rs = stmt.executeQuery();

				if (rs.next()) {
					String vendorString = rs.getString("VENDOR").toLowerCase();
					if (!wimVendors.contains(vendorString)) {
						wimVendors.add(vendorString);
					}

				}
			}
            if (wimVendors.isEmpty()) {
                Logger.error("Error retrieving the Wims Vendor");
                wimVendors = null;
            }
		} catch (SQLException e) {
			Logger.error(e.getMessage());
			wimVendors = null;
		} catch (ClassNotFoundException e) {
			Logger.error(e.getMessage(), e);
			wimVendors = null;
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
				wimVendors = null;
			}
		}
		  
		return wimVendors;
	}

    /**
     * Retrieve the WIM UUIDs managing connectivity in for the serviced given net segment.
     *
     * @param vimUuid the UUID of the vim attached to wim
     *
     * @return an array of String objects representing the UUID of the WIMs
     */
    private ArrayList<String> readWimEntryFromVimUuid(String vimUuid) {

        ArrayList<String> uuids = new ArrayList<String>();

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("org.postgresql.Driver");
            connection =
                    DriverManager.getConnection(
                            "jdbc:postgresql://" + prop.getProperty("repo_host") + ":"
                                    + prop.getProperty("repo_port") + "/" + "wimregistry",
                            prop.getProperty("user"), prop.getProperty("pass"));
            connection.setAutoCommit(false);

            stmt = connection.prepareStatement(
                    "SELECT * FROM wim,attached_vim WHERE wim.uuid = attached_vim.wim_uuid AND attached_vim.vim_uuid=?;");
            stmt.setString(1, vimUuid);
            rs = stmt.executeQuery();

            if (rs.next()) {
                uuids.add(rs.getString("UUID"));
            } else {
                Logger.error("Error retrieving the Wims Uuids");
                uuids = null;
            }
        } catch (SQLException e) {
            Logger.error(e.getMessage(), e);
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
                Logger.error(e.getMessage(), e);
                uuids = null;

            }
        }
        return uuids;

    }

    /**
     * Return a list of the WIMs hosting at least one Service Instance.
     *
     * @param instanceUuid the UUID that identifies the Service Instance
     * @return an array of String objecst representing the UUID of the WIMs
     */
    private ArrayList<String> getWimUuidFromInstance(String instanceUuid) {

        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ArrayList<String> uuids = new ArrayList<String>();

        try {
            Class.forName("org.postgresql.Driver");
            connection =
                    DriverManager.getConnection(
                            "jdbc:postgresql://" + prop.getProperty("repo_host") + ":"
                                    + prop.getProperty("repo_port") + "/" + "wimregistry",
                            prop.getProperty("user"), prop.getProperty("pass"));
            connection.setAutoCommit(false);

            stmt = connection
                    .prepareStatement("SELECT WIM_UUID FROM service_instances  WHERE INSTANCE_UUID=?;");
            stmt.setString(1, instanceUuid);
            rs = stmt.executeQuery();

            while (rs.next()) {
                uuids.add(rs.getString("WIM_UUID"));
            }
            if (uuids.isEmpty()) {
                Logger.error("Error retrieving the Wims Uuids");
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


}
