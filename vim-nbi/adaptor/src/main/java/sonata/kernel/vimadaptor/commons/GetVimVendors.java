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

package sonata.kernel.vimadaptor.commons;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

//import sonata.kernel.vimadaptor.commons.ServicePreparePayload;
//import sonata.kernel.vimadaptor.commons.SonataManifestMapper;
//import sonata.kernel.vimadaptor.commons.VimPreDeploymentList;
import sonata.kernel.vimadaptor.messaging.ServicePlatformMessage;
import sonata.kernel.vimadaptor.wrapper.VimRepo;

//import org.json.JSONObject;
//import org.json.JSONTokener;

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
		} else {
			return null;
		}
		  
	}
  
	/**
	 * Retrieve the vim Vendors from the info in the message received for service prepare.
	 * 
	 * @param message the message received from RebbitMQ
	 *
	 * @return the list of vim Vendors or null 
	 */
	private ArrayList<String> GetVimVendorsPrepare(ServicePlatformMessage message) {
		  
		Logger.info("Call received - sid: " + message.getSid());
		// parse the payload to get VIM UUID from the request body
		Logger.info("Parsing payload...");
		ServicePreparePayload payload = null;
		ObjectMapper mapper = SonataManifestMapper.getSonataMapper();
		ArrayList<String> vimUuids = null;
		ArrayList<String> vimVendors = null;

		try {
			payload = mapper.readValue(message.getBody(), ServicePreparePayload.class);
			Logger.info("payload parsed. Get VIMs");

	        for (VimPreDeploymentList vim : payload.getVimList()) {
                vimUuids.add(vim.getUuid());
            }
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
	 * Retrieve the vim Vendors from the vim uuids consulting the DB.
	 * 
	 * @param vimUuids the vim uuids
	 *
	 * @return the list of vim Vendors or null 
	 */
	private ArrayList<String> GetVimVendorsDB(ArrayList<String> vimUuids) {
		  
		ArrayList<String> vimVendors = null; 
		  
		
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
					String vendorString = rs.getString("VENDOR").toUpperCase();
					if (!vimVendors.contains(vendorString)) {
						vimVendors.add(vendorString);						
					}

				}
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

}