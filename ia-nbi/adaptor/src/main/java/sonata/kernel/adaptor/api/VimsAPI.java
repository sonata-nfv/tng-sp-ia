package sonata.kernel.adaptor.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.LoggerFactory;
import sonata.kernel.adaptor.commons.SonataManifestMapper;
import sonata.kernel.adaptor.wrapper.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.UUID;

@Path("/vims")
public class VimsAPI {

  private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(VimsAPI.class);

  /**
   * api call in order to get a list of the registered VIMs
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVims(@QueryParam("type") String type) {

    ArrayList<VimApiConfiguration> output = new ArrayList<>();
    Response.ResponseBuilder apiResponse = null;
    try {
      ArrayList<String> vimUuids = WrapperBay.getInstance().getComputeWrapperList();
      vimUuids.addAll(WrapperBay.getInstance().getNepList());
      Logger.info("Found " + vimUuids.size() + " VIMs");
      Logger.info("Retrieving VIM(s)");
      VimWrapperConfiguration vimWrapperConfig = null;
      VimWrapperConfiguration netVimWrapperConfig = null;
      VimApiConfiguration vimApiConfig = null;
      for (String vimUuid : vimUuids) {
        vimWrapperConfig = WrapperBay.getInstance().getConfig(vimUuid);

        // If query by type, filter the type supplied
        if (type != null) {
          if (ComputeVimVendor.getPossibleVendors().contains(type) && (vimWrapperConfig.getVimVendor() != ComputeVimVendor.getByName(type))) {
            continue;
          } else if (EndpointVimVendor.getPossibleVendors().contains(type) && (vimWrapperConfig.getVimVendor() != EndpointVimVendor.getByName(type))) {
            continue;
          }
        }

        String netVimUuid = WrapperBay.getInstance().getVimRepo().getNetworkVimUuidFromComputeVimUuid(vimUuid);
        if (netVimUuid != null) {
          netVimWrapperConfig = WrapperBay.getInstance().getConfig(netVimUuid);
        } else {
          netVimWrapperConfig = null;
        }
        if ((vimWrapperConfig != null) && (netVimWrapperConfig != null)) {
          vimApiConfig = getVimApiFromWrapperApi(vimWrapperConfig);
          vimApiConfig.setNetworkEndpoint(netVimWrapperConfig.getVimEndpoint());
          output.add(vimApiConfig);
        } else if ((vimWrapperConfig != null) && vimWrapperConfig.getWrapperType().equals(WrapperType.getByName("endpoint"))) {
          vimApiConfig = getVimApiFromWrapperApi(vimWrapperConfig);
          output.add(vimApiConfig);
        }
      }
      ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
      String body = mapper.writeValueAsString(output);

      Logger.info("List VIM call completed.");
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(200).build();

    } catch (Exception e) {
      Logger.error("Error getting the vim(s): " + e.getMessage(), e);
      String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIMs\"}";
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(404).build();
    }

  }

  /**
   * api call in order to get a specific registered VIM
   */
  @GET
  @Path("/{vimUuid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVim(@PathParam("vimUuid") String vimUuid) {

    Response.ResponseBuilder apiResponse = null;
    try {
      Logger.info("Retrieving VIM");
      VimWrapperConfiguration vimWrapperConfig = WrapperBay.getInstance().getConfig(vimUuid);
      String netVimUuid = WrapperBay.getInstance().getVimRepo().getNetworkVimUuidFromComputeVimUuid(vimUuid);
      VimWrapperConfiguration netVimWrapperConfig = null;
      if (netVimUuid != null) {
        netVimWrapperConfig = WrapperBay.getInstance().getConfig(netVimUuid);
      }

      if ((vimWrapperConfig == null) || ((netVimWrapperConfig == null) && !vimWrapperConfig.getWrapperType().equals(WrapperType.getByName("endpoint")))) {
        Logger.error("Not Found VIM UUID " + vimUuid);
        String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM UUID " + vimUuid + "\"}";
        apiResponse = Response.ok((String) body);
        apiResponse.header("Content-Length", body.length());
        return apiResponse.status(404).build();
      }

      VimApiConfiguration vimApiConfig = getVimApiFromWrapperApi(vimWrapperConfig);
      if (netVimWrapperConfig != null) {
        vimApiConfig.setNetworkEndpoint(netVimWrapperConfig.getVimEndpoint());
      }
      ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
      String body = mapper.writeValueAsString(vimApiConfig);

      Logger.info("Get VIM call completed.");
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(200).build();

    } catch (Exception e) {
      Logger.error("Error getting the vim: " + e.getMessage(), e);
      String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM\"}";
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(404).build();
    }

  }

  /**
   * api call in order to register a VIM
   */
  @POST
  @Path("/{type}")
  //@SuppressWarnings("null")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addVim(@PathParam("type") String type, String newVim) {

    ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
    Response.ResponseBuilder apiResponse = null;
    Logger.debug("VIM Configuration received: " + newVim);

    try {
      VimApiConfiguration vimApiConfig = mapper.readValue(newVim, VimApiConfiguration.class);
      Logger.info("Try Retrieving VIM");
      VimWrapperConfiguration vimComputeWrapperConfig = WrapperBay.getInstance().getConfig(vimApiConfig.getUuid());

      if (vimComputeWrapperConfig != null) {
        Logger.error("VIM " + vimApiConfig.getUuid() + " already exist");
        String body = "{\"status\":\"ERROR\",\"message\":\"VIM " + vimApiConfig.getUuid() + " already exist\"}";
        apiResponse = Response.ok((String) body);
        apiResponse.header("Content-Length", body.length());
        return apiResponse.status(405).build();
      }

      Logger.debug("Registering a VIM wrapper.");
      vimComputeWrapperConfig = getVimWrapperFromVimApi(vimApiConfig);
      if (type.equals("endpoint")) {
        vimComputeWrapperConfig.setWrapperType(WrapperType.getByName("endpoint"));
        vimComputeWrapperConfig.setVimVendor(EndpointVimVendor.getByName(type));
      } else {
        vimComputeWrapperConfig.setWrapperType(WrapperType.getByName("compute"));
        vimComputeWrapperConfig.setVimVendor(ComputeVimVendor.getByName(type));
      }
      if (vimComputeWrapperConfig.getUuid() == null) {
        vimComputeWrapperConfig.setUuid(UUID.randomUUID().toString());
      }
      if (type.equals("heat")) {
        if (vimComputeWrapperConfig.getDomain() == null) {
          vimComputeWrapperConfig.setDomain("Default");
        }
      }
      WrapperBay.getInstance().registerComputeWrapper(vimComputeWrapperConfig);

      //Register ovs network wrapper or use mock
      VimWrapperConfiguration vimNetworkWrapperConfig = new VimWrapperConfiguration();
      if (vimApiConfig.getNetworkEndpoint() != null) {
        // Register ovs network wrapper
        if (!vimApiConfig.getNetworkEndpoint().equals("")) {
          vimNetworkWrapperConfig.setUuid(UUID.randomUUID().toString());
          vimNetworkWrapperConfig.setName(vimComputeWrapperConfig.getName() + "-Net");
          vimNetworkWrapperConfig.setWrapperType(WrapperType.getByName("network"));
          vimNetworkWrapperConfig.setVimVendor(NetworkVimVendor.getByName("ovs"));
          vimNetworkWrapperConfig.setVimEndpoint(vimApiConfig.getNetworkEndpoint());
          //vimNetworkWrapperConfig.setAuthUserName("");
          //vimNetworkWrapperConfig.setDomain("");
          //vimNetworkWrapperConfig.setConfiguration("{}");
          WrapperBay.getInstance().registerNetworkWrapper(vimNetworkWrapperConfig, vimComputeWrapperConfig.getUuid());
        }
      }
      // Use mock network wrapper
      if ((vimNetworkWrapperConfig.getVimEndpoint() == null) && !type.equals("endpoint")) {
        vimNetworkWrapperConfig.setUuid(UUID.randomUUID().toString());
        vimNetworkWrapperConfig.setName("Mock-Net");
        vimNetworkWrapperConfig.setWrapperType(WrapperType.getByName("network"));
        vimNetworkWrapperConfig.setVimVendor(NetworkVimVendor.getByName("networkmock"));
        vimNetworkWrapperConfig.setVimEndpoint("");
        //vimNetworkWrapperConfig.setAuthUserName("");
        //vimNetworkWrapperConfig.setDomain("");
        //vimNetworkWrapperConfig.setConfiguration("{}");
        WrapperBay.getInstance().registerNetworkWrapper(vimNetworkWrapperConfig, vimComputeWrapperConfig.getUuid());
      }

      Logger.info("Retrieving new VIM");
      vimComputeWrapperConfig = WrapperBay.getInstance().getConfig(vimComputeWrapperConfig.getUuid());
      String netVimUuid = null;
      if (vimComputeWrapperConfig != null) {
        netVimUuid = WrapperBay.getInstance().getVimRepo().getNetworkVimUuidFromComputeVimUuid(vimComputeWrapperConfig.getUuid());
      }
      if (netVimUuid != null) {
        vimNetworkWrapperConfig = WrapperBay.getInstance().getConfig(netVimUuid);
      } else {
        vimNetworkWrapperConfig = null;
      }
      if ((vimComputeWrapperConfig == null) || ((vimNetworkWrapperConfig == null) && !type.equals("endpoint"))) {
        Logger.error("VIM register failed");
        String body = "{\"status\":\"ERROR\",\"message\":\"VIM register failed\"}";
        apiResponse = Response.ok((String) body);
        apiResponse.header("Content-Length", body.length());
        return apiResponse.status(400).build();
      }
      vimApiConfig = getVimApiFromWrapperApi(vimComputeWrapperConfig);
      if (vimNetworkWrapperConfig != null) {
        vimApiConfig.setNetworkEndpoint(vimNetworkWrapperConfig.getVimEndpoint());
      }
      String body = mapper.writeValueAsString(vimApiConfig);

      Logger.info("Add VIM call completed.");
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(201).build();

    } catch (Exception e) {
      Logger.error("Error adding the vim: " + e.getMessage(), e);
      String body = "{\"status\":\"ERROR\",\"message\":\"Message malformed, or missing fields, need to be json\"}";
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(400).build();
    }

  }

  /**
   * api call in order to update a specific VIM
   */
  @PATCH
  @Path("/{type}/{vimUuid}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateVim(@PathParam("type") String type, @PathParam("vimUuid") String vimUuid, String updateVim) {

    ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
    Response.ResponseBuilder apiResponse = null;
    Logger.debug("VIM Update Configuration received: " + updateVim);

    try {
      Logger.info("Retrieving VIM");
      VimWrapperConfiguration vimComputeWrapperConfig = WrapperBay.getInstance().getConfig(vimUuid);
      String netVimUuid = WrapperBay.getInstance().getVimRepo().getNetworkVimUuidFromComputeVimUuid(vimUuid);
      VimWrapperConfiguration vimNetworkWrapperConfig = null;
      if (netVimUuid != null) {
        vimNetworkWrapperConfig = WrapperBay.getInstance().getConfig(netVimUuid);
      }

      if ((vimComputeWrapperConfig == null) || ((vimNetworkWrapperConfig == null) && !type.equals("endpoint"))) {
        Logger.error("Not Found VIM UUID " + vimUuid);
        String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM UUID " + vimUuid + "\"}";
        apiResponse = Response.ok((String) body);
        apiResponse.header("Content-Length", body.length());
        return apiResponse.status(404).build();
      }

      VimApiConfiguration vimApiConfig = getVimApiFromWrapperApi(vimComputeWrapperConfig);
      if (vimNetworkWrapperConfig != null) {
        vimApiConfig.setNetworkEndpoint(vimNetworkWrapperConfig.getVimEndpoint());
      }

      VimApiConfiguration vimApiUpdateConfig = mapper.readValue(updateVim, VimApiConfiguration.class);

      Logger.info("Update VIM information");
      updateVimApi(vimApiConfig,vimApiUpdateConfig);


      Logger.debug("Update VIM in the DB.");
      vimComputeWrapperConfig = getVimWrapperFromVimApi(vimApiConfig);
      if (type.equals("endpoint")) {
        vimComputeWrapperConfig.setWrapperType(WrapperType.getByName("endpoint"));
        vimComputeWrapperConfig.setVimVendor(EndpointVimVendor.getByName(type));
      } else {
        vimComputeWrapperConfig.setWrapperType(WrapperType.getByName("compute"));
        vimComputeWrapperConfig.setVimVendor(ComputeVimVendor.getByName(type));
      }

      // If update the uuid, needs to delete and insert in the vim_link
      if (!vimUuid.equals(vimComputeWrapperConfig.getUuid()) && !type.equals("endpoint")) {
        WrapperBay.getInstance().getVimRepo().removeNetworkVimLink(vimNetworkWrapperConfig.getUuid());
        WrapperBay.getInstance().getVimRepo().updateVimEntry(vimUuid, vimComputeWrapperConfig);
        WrapperBay.getInstance().getVimRepo().writeNetworkVimLink(vimComputeWrapperConfig.getUuid(), vimNetworkWrapperConfig.getUuid());
      } else {
        WrapperBay.getInstance().getVimRepo().updateVimEntry(vimUuid, vimComputeWrapperConfig);
      }
      //Register ovs network wrapper or use mock
      boolean flagMock = true;
      if ((vimApiConfig.getNetworkEndpoint() != null && !type.equals("endpoint"))) {
        // Register ovs network wrapper
        if (!vimApiConfig.getNetworkEndpoint().equals("")) {
          vimNetworkWrapperConfig.setName(vimComputeWrapperConfig.getName() + "-Net");
          vimNetworkWrapperConfig.setVimVendor(NetworkVimVendor.getByName("ovs"));
          vimNetworkWrapperConfig.setVimEndpoint(vimApiConfig.getNetworkEndpoint());
          WrapperBay.getInstance().getVimRepo().updateVimEntry(vimNetworkWrapperConfig.getUuid(),vimNetworkWrapperConfig);
          flagMock = false;
        }
      }
      // Use mock network wrapper
      if (flagMock && !type.equals("endpoint")) {
        vimNetworkWrapperConfig.setName("Mock-Net");
        vimNetworkWrapperConfig.setVimVendor(NetworkVimVendor.getByName("networkmock"));
        vimNetworkWrapperConfig.setVimEndpoint("");
        WrapperBay.getInstance().getVimRepo().updateVimEntry(vimNetworkWrapperConfig.getUuid(),vimNetworkWrapperConfig);

      }

      Logger.info("Retrieving updated VIM");
      vimComputeWrapperConfig = WrapperBay.getInstance().getConfig(vimComputeWrapperConfig.getUuid());
      netVimUuid = null;
      if (vimComputeWrapperConfig != null) {
        netVimUuid = WrapperBay.getInstance().getVimRepo().getNetworkVimUuidFromComputeVimUuid(vimComputeWrapperConfig.getUuid());
      }
      if (netVimUuid != null) {
        vimNetworkWrapperConfig = WrapperBay.getInstance().getConfig(netVimUuid);
      } else {
        vimNetworkWrapperConfig = null;
      }
      if ((vimComputeWrapperConfig == null) || ((vimNetworkWrapperConfig == null) && !type.equals("endpoint"))) {
        Logger.error("VIM register failed");
        String body = "{\"status\":\"ERROR\",\"message\":\"VIM register failed\"}";
        apiResponse = Response.ok((String) body);
        apiResponse.header("Content-Length", body.length());
        return apiResponse.status(400).build();
      }
      vimApiConfig = getVimApiFromWrapperApi(vimComputeWrapperConfig);
      if (vimNetworkWrapperConfig != null) {
        vimApiConfig.setNetworkEndpoint(vimNetworkWrapperConfig.getVimEndpoint());
      }
      String body = mapper.writeValueAsString(vimApiConfig);

      Logger.info("Add VIM call completed.");
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(201).build();

    } catch (Exception e) {
      Logger.error("Error adding the vim: " + e.getMessage(), e);
      String body = "{\"status\":\"ERROR\",\"message\":\"Message malformed, or missing fields, need to be json\"}";
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(400).build();
    }

  }

  /**
   * api call in order to delete a specific registered VIM
   */
  @DELETE
  @Path("/{vimUuid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteVim(@PathParam("vimUuid") String vimUuid) {

    Response.ResponseBuilder apiResponse = null;
    try {
      Logger.info("Retrieving VIM");
      VimWrapperConfiguration vim = WrapperBay.getInstance().getConfig(vimUuid);
      String netVimUuid = WrapperBay.getInstance().getVimRepo().getNetworkVimUuidFromComputeVimUuid(vimUuid);

      if ((vim == null) || ((netVimUuid == null) && !vim.getWrapperType().equals(WrapperType.getByName("endpoint")))) {
        Logger.error("Not Found VIM UUID " + vimUuid);
        String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM UUID " + vimUuid + "\"}";
        apiResponse = Response.ok((String) body);
        apiResponse.header("Content-Length", body.length());
        return apiResponse.status(404).build();
      }
      if (netVimUuid != null) {
        WrapperBay.getInstance().removeNetworkWrapper(netVimUuid);
      }
      WrapperBay.getInstance().removeComputeWrapper(vimUuid);

      Logger.info("Delete VIM call completed.");
      String body = "{\"status\":\"SUCCESS\",\"message\":\"Deleted VIM UUID " + vimUuid + "\"}";
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(200).build();

    } catch (Exception e) {
      Logger.error("Error getting the vim: " + e.getMessage(), e);
      String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM\"}";
      apiResponse = Response.ok((String) body);
      apiResponse.header("Content-Length", body.length());
      return apiResponse.status(404).build();
    }

  }

  /**
   * Returns a VimWrapperConfiguration translated from the VimApiConfiguration
   *
   * @param vimApiConfig the user data received from api
   * @return VimWrapperConfiguration object translated from the given data
   * @throws Exception if unable to translate
   */
  private VimWrapperConfiguration getVimWrapperFromVimApi(VimApiConfiguration vimApiConfig) throws Exception {
    VimWrapperConfiguration vimWrapperConfig = new VimWrapperConfiguration();

    if (vimApiConfig.getUuid() != null) {
      vimWrapperConfig.setUuid(vimApiConfig.getUuid());
    }
    if (vimApiConfig.getName() != null) {
      vimWrapperConfig.setName(vimApiConfig.getName());
    }
    if (vimApiConfig.getCountry() != null) {
      vimWrapperConfig.setCountry(vimApiConfig.getCountry());
    }
    if (vimApiConfig.getCity() != null) {
      vimWrapperConfig.setCity(vimApiConfig.getCity());
    }
    if (vimApiConfig.getEndpoint() != null) {
      vimWrapperConfig.setVimEndpoint(vimApiConfig.getEndpoint());
    }
    if (vimApiConfig.getUserName() != null) {
      vimWrapperConfig.setAuthUserName(vimApiConfig.getUserName());
    }
    if (vimApiConfig.getPassword() != null) {
      vimWrapperConfig.setAuthPass(vimApiConfig.getPassword());
    }
    if (vimApiConfig.getAuthKey() != null) {
      vimWrapperConfig.setAuthKey(vimApiConfig.getAuthKey());
    }
    if (vimApiConfig.getDomain() != null) {
      vimWrapperConfig.setDomain(vimApiConfig.getDomain());
    }
    // If have configuration (k8s)
    if (vimApiConfig.getConfiguration() != null) {
      vimWrapperConfig.setConfiguration(vimApiConfig.getConfiguration().toString());
    } else {
      //Construct configuration json (heat)
      String config = null;
      if (vimApiConfig.getTenant() != null) {
        if (config == null) {
          config = "{";
        } else {
          config = config + ", ";
        }
        config = config + "\"tenant\":\"" + vimApiConfig.getTenant() + "\"";
      }
      if (vimApiConfig.getPrivateNetworkPrefix() != null) {
        if (config == null) {
          config = "{";
        } else {
          config = config + ", ";
        }
        config = config + "\"tenant_private_net_id\":\"" + vimApiConfig.getPrivateNetworkPrefix() + "\"";
      }
      if (vimApiConfig.getPrivateNetworkLength() != null) {
        if (config == null) {
          config = "{";
        } else {
          config = config + ", ";
        }
        config = config + "\"tenant_private_net_length\":\"" + vimApiConfig.getPrivateNetworkLength() + "\"";
      }
      if (vimApiConfig.getExternalNetworkId() != null) {
        if (config == null) {
          config = "{";
        } else {
          config = config + ", ";
        }
        config = config + "\"tenant_ext_net\":\"" + vimApiConfig.getExternalNetworkId() + "\"";
      }
      if (vimApiConfig.getExternalRouterId() != null) {
        if (config == null) {
          config = "{";
        } else {
          config = config + ", ";
        }
        config = config + "\"tenant_ext_router\":\"" + vimApiConfig.getExternalRouterId() + "\"";
      }
      if (config == null) {
        config = "{}";
      } else {
        config = config + "}";
      }

      vimWrapperConfig.setConfiguration(config);
    }

    return vimWrapperConfig;
  }

  /**
   * Returns a VimApiConfiguration translated from the VimWrapperConfiguration
   *
   * @param vimWrapperConfig the wrapper data received from the db
   * @return VimApiConfiguration object translated from the given data
   * @throws Exception if unable to translate
   */
  private VimApiConfiguration getVimApiFromWrapperApi(VimWrapperConfiguration vimWrapperConfig) throws Exception {
    VimApiConfiguration vimApiConfig = new VimApiConfiguration();

    if (vimWrapperConfig.getUuid() != null) {
      vimApiConfig.setUuid(vimWrapperConfig.getUuid());
    }
    if (vimWrapperConfig.getVimVendor() != null) {
      vimApiConfig.setType(vimWrapperConfig.getVimVendor().toString());
    }
    if (vimWrapperConfig.getName() != null) {
      vimApiConfig.setName(vimWrapperConfig.getName());
    }
    if (vimWrapperConfig.getCountry() != null) {
      vimApiConfig.setCountry(vimWrapperConfig.getCountry());
    }
    if (vimWrapperConfig.getCity() != null) {
      vimApiConfig.setCity(vimWrapperConfig.getCity());
    }
    if (vimWrapperConfig.getVimEndpoint() != null) {
      vimApiConfig.setEndpoint(vimWrapperConfig.getVimEndpoint());
    }
    if (vimWrapperConfig.getAuthUserName() != null) {
      vimApiConfig.setUserName(vimWrapperConfig.getAuthUserName());
    }
    if (vimWrapperConfig.getAuthPass() != null) {
      vimApiConfig.setPassword(vimWrapperConfig.getAuthPass());
    }
    if (vimWrapperConfig.getAuthKey() != null) {
      vimApiConfig.setAuthKey(vimWrapperConfig.getAuthKey());
    }
    if (vimWrapperConfig.getDomain() != null) {
      vimApiConfig.setDomain(vimWrapperConfig.getDomain());
    }
    if (vimWrapperConfig.getConfiguration() != null) {
      //If is heat, parse configuration json
      if (vimWrapperConfig.getVimVendor() == ComputeVimVendor.getByName("heat")) {
        JSONTokener tokener = new JSONTokener(vimWrapperConfig.getConfiguration());
        JSONObject object = (JSONObject) tokener.nextValue();
        if (object.has("tenant")) {
          vimApiConfig.setTenant(object.getString("tenant"));
        }
        if (object.has("tenant_private_net_id")) {
          vimApiConfig.setPrivateNetworkPrefix(object.getString("tenant_private_net_id"));
        }
        if (object.has("tenant_private_net_length")) {
          vimApiConfig.setPrivateNetworkLength(object.getString("tenant_private_net_length"));
        }
        if (object.has("tenant_ext_net")) {
          vimApiConfig.setExternalNetworkId(object.getString("tenant_ext_net"));
        }
        if (object.has("tenant_ext_router")) {
          vimApiConfig.setExternalRouterId(object.getString("tenant_ext_router"));
        }
      } else {
        ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
        vimApiConfig.setConfiguration(mapper.readTree(vimWrapperConfig.getConfiguration()));
      }
    }

    return vimApiConfig;
  }

  /**
   * Returns a VimApiConfiguration updated from the VimApiConfiguration sends by user
   *
   * @param vimApiConfig the user data stored in DB
   * @param vimUpdateApiConfig the updated user data received from api
   * @return VimApiConfiguration object updated from the given data
   * @throws Exception if unable to update
   */
  private void updateVimApi(VimApiConfiguration vimApiConfig, VimApiConfiguration vimUpdateApiConfig) throws Exception {

    if (vimUpdateApiConfig.getUuid() != null) {
      vimApiConfig.setUuid(vimUpdateApiConfig.getUuid());
    }
    if (vimUpdateApiConfig.getName() != null) {
      vimApiConfig.setName(vimUpdateApiConfig.getName());
    }
    if (vimUpdateApiConfig.getCountry() != null) {
      vimApiConfig.setCountry(vimUpdateApiConfig.getCountry());
    }
    if (vimUpdateApiConfig.getCity() != null) {
      vimApiConfig.setCity(vimUpdateApiConfig.getCity());
    }
    if (vimUpdateApiConfig.getEndpoint() != null) {
      vimApiConfig.setEndpoint(vimUpdateApiConfig.getEndpoint());
    }
    if (vimUpdateApiConfig.getUserName() != null) {
      vimApiConfig.setUserName(vimUpdateApiConfig.getUserName());
    }
    if (vimUpdateApiConfig.getPassword() != null) {
      vimApiConfig.setPassword(vimUpdateApiConfig.getPassword());
    }
    if (vimUpdateApiConfig.getAuthKey() != null) {
      vimApiConfig.setAuthKey(vimUpdateApiConfig.getAuthKey());
    }
    if (vimUpdateApiConfig.getDomain() != null) {
      vimApiConfig.setDomain(vimUpdateApiConfig.getDomain());
    }
    if (vimUpdateApiConfig.getTenant() != null) {
      vimApiConfig.setTenant(vimUpdateApiConfig.getTenant());
    }
    if (vimUpdateApiConfig.getPrivateNetworkPrefix() != null) {
      vimApiConfig.setPrivateNetworkPrefix(vimUpdateApiConfig.getPrivateNetworkPrefix());
    }
    if (vimUpdateApiConfig.getPrivateNetworkLength() != null) {
      vimApiConfig.setPrivateNetworkLength(vimUpdateApiConfig.getPrivateNetworkLength());
    }
    if (vimUpdateApiConfig.getExternalNetworkId() != null) {
      vimApiConfig.setExternalNetworkId(vimUpdateApiConfig.getExternalNetworkId());
    }
    if (vimUpdateApiConfig.getExternalRouterId() != null) {
      vimApiConfig.setExternalRouterId(vimUpdateApiConfig.getExternalRouterId());
    }
    if (vimUpdateApiConfig.getNetworkEndpoint() != null) {
      vimApiConfig.setNetworkEndpoint(vimUpdateApiConfig.getNetworkEndpoint());
    }
    if (vimUpdateApiConfig.getConfiguration() != null) {
      vimApiConfig.setConfiguration(vimUpdateApiConfig.getConfiguration());
    }
    //return vimApiConfig;
  }

}
