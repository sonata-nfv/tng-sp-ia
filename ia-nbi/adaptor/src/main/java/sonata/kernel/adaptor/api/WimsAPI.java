package sonata.kernel.adaptor.api;

import com.fasterxml.jackson.databind.ObjectMapper;
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

@Path("/wims")
public class WimsAPI {

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(WimsAPI.class);

    /**
     * api call in order to get a list of the registered WIMs
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWims() {

        ArrayList<WimApiConfiguration> output = new ArrayList<>();
        Response.ResponseBuilder apiResponse = null;
        try {
            ArrayList<String> wimUuids = WrapperBay.getInstance().getWimList();
            Logger.info("Found " + wimUuids.size() + " WIMs");
            Logger.info("Retrieving WIM(s)");
            WimWrapperConfiguration wimWrapperConfig = null;
            WimApiConfiguration wimApiConfig = null;
            for (String wimUuid : wimUuids) {
                wimWrapperConfig = WrapperBay.getInstance().getWimConfigFromWimUuid(wimUuid);
                ArrayList<String> attachedVims = WrapperBay.getInstance().getAttachedVims(wimUuid);
                if ((wimWrapperConfig != null) && (attachedVims != null)) {
                    wimWrapperConfig.setAttachedVims(attachedVims);
                    wimApiConfig = getWimApiFromWrapperApi(wimWrapperConfig);
                    output.add(wimApiConfig);
                }
            }
            ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
            String body = mapper.writeValueAsString(output);

            Logger.info("List WIM call completed.");
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(200).build();

        } catch (Exception e) {
            Logger.error("Error getting the wim(s): " + e.getMessage(), e);
            String body = "{\"status\":\"ERROR\",\"message\":\"Not Found WIMs\"}";
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(404).build();
        }

    }

    /**
     * api call in order to get a specific registered WIM
     */
    @GET
    @Path("/{wimUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWim(@PathParam("wimUuid") String wimUuid) {

        Response.ResponseBuilder apiResponse = null;
        try {
            Logger.info("Retrieving WIM");
            WimWrapperConfiguration wimWrapperConfig = WrapperBay.getInstance().getWimConfigFromWimUuid(wimUuid);
            ArrayList<String> attachedVims = WrapperBay.getInstance().getAttachedVims(wimUuid);

            if ((wimWrapperConfig == null) || (attachedVims == null)) {
                Logger.error("Not Found WIM UUID " + wimUuid);
                String body = "{\"status\":\"ERROR\",\"message\":\"Not Found WIM UUID " + wimUuid + "\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(404).build();
            }

            wimWrapperConfig.setAttachedVims(attachedVims);
            WimApiConfiguration wimApiConfig = getWimApiFromWrapperApi(wimWrapperConfig);
            ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
            String body = mapper.writeValueAsString(wimApiConfig);

            Logger.info("Get WIM call completed.");
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(200).build();

        } catch (Exception e) {
            Logger.error("Error getting the wim: " + e.getMessage(), e);
            String body = "{\"status\":\"ERROR\",\"message\":\"Not Found WIM\"}";
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(404).build();
        }

    }

    /**
     * api call in order to register a WIM
     */
    @POST
    @Path("/{type}")
    //@SuppressWarnings("null")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addWim(@PathParam("type") String type, String newWim) {

        ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
        Response.ResponseBuilder apiResponse = null;
        Logger.debug("WIM Configuration received: ");
        System.out.println(newWim);

        try {
            WimApiConfiguration wimApiConfig = mapper.readValue(newWim, WimApiConfiguration.class);
            Logger.info("Try Retrieving WIM");
            WimWrapperConfiguration wimWrapperConfig = WrapperBay.getInstance().getWimConfigFromWimUuid(wimApiConfig.getUuid());

            if (wimWrapperConfig != null) {
                Logger.error("WIM " + wimApiConfig.getUuid() + " already exist");
                String body = "{\"status\":\"ERROR\",\"message\":\"WIM " + wimApiConfig.getUuid() + " already exist\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(405).build();
            }

            Logger.debug("Registering a WIM wrapper.");
            wimWrapperConfig = getWimWrapperFromVimApi(wimApiConfig);
            wimWrapperConfig.setWimVendor(WimVendor.getByName(type));
            if (wimWrapperConfig.getUuid() == null) {
                wimWrapperConfig.setUuid(UUID.randomUUID().toString());
            }

            //Get Vims address, check if exist, and check if is not already associated with wim
            ArrayList<VimWrapperConfiguration> vims = new ArrayList<>();
            VimWrapperConfiguration vimComputeWrapperConfig;
            String wimUuid;
            if (wimWrapperConfig.getAttachedVims() != null) {
                for (String vimUuid : wimWrapperConfig.getAttachedVims()) {
                    wimUuid = WrapperBay.getInstance().getWimRepo().getWimUuidFromVimUuid(vimUuid);
                    if (wimUuid != null) {
                        Logger.error("VIM " + vimUuid + " already attached to WIM " + wimUuid);
                        String body = "{\"status\":\"ERROR\",\"message\":\"VIM " + vimUuid + " already attached to WIM " + wimUuid + "\"}";
                        apiResponse = Response.ok((String) body);
                        apiResponse.header("Content-Length", body.length());
                        return apiResponse.status(405).build();
                    }

                    vimComputeWrapperConfig = WrapperBay.getInstance().getConfig(vimUuid);
                    if (vimComputeWrapperConfig == null) {
                        Logger.error("VIM " + vimUuid + " not exist");
                        String body = "{\"status\":\"ERROR\",\"message\":\"VIM " + vimUuid + " not exist\"}";
                        apiResponse = Response.ok((String) body);
                        apiResponse.header("Content-Length", body.length());
                        return apiResponse.status(405).build();
                    }
                    vims.add(vimComputeWrapperConfig);
                }
            }

            WrapperBay.getInstance().registerWimWrapper(wimWrapperConfig);

            //Attach Vims to the Wim registered
            if (wimWrapperConfig.getAttachedVims() != null) {
                for (VimWrapperConfiguration vim : vims) {
                    WrapperBay.getInstance().attachVim(wimWrapperConfig.getUuid(), vim.getUuid(), vim.getVimEndpoint());
                }
            }

            Logger.info("Retrieving new WIM");
            wimWrapperConfig = WrapperBay.getInstance().getWimConfigFromWimUuid(wimWrapperConfig.getUuid());
            ArrayList<String> attachedVims = null;
            if (wimWrapperConfig != null) {
                attachedVims = WrapperBay.getInstance().getAttachedVims(wimWrapperConfig.getUuid());
            }

            if ((wimWrapperConfig == null) || (attachedVims == null)) {
                Logger.error("WIM register failed");
                String body = "{\"status\":\"ERROR\",\"message\":\"WIM register failed\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(400).build();
            }
            wimWrapperConfig.setAttachedVims(attachedVims);
            wimApiConfig = getWimApiFromWrapperApi(wimWrapperConfig);
            String body = mapper.writeValueAsString(wimApiConfig);

            Logger.info("Add WIM call completed.");
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(201).build();

        } catch (Exception e) {
            Logger.error("Error adding the wim: " + e.getMessage(), e);
            String body = "{\"status\":\"ERROR\",\"message\":\"Message malformed, or missing fields, need to be json\"}";
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(400).build();
        }

    }

    /**
     * api call in order to update a specific WIM
     */
    @PATCH
    @Path("/{type}/{wimUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateWim(@PathParam("type") String type, @PathParam("wimUuid") String wimUuid, String updateWim) {

        ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
        Response.ResponseBuilder apiResponse = null;
        Logger.debug("WIM Update Configuration received: ");
        System.out.println(updateWim);

        try {
            Logger.info("Retrieving WIM");
            WimWrapperConfiguration wimWrapperConfig = WrapperBay.getInstance().getWimConfigFromWimUuid(wimUuid);
            if (wimWrapperConfig == null) {
                Logger.error("Not Found WIM UUID " + wimUuid);
                String body = "{\"status\":\"ERROR\",\"message\":\"Not Found WIM UUID " + wimUuid + "\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(404).build();
            }


            ArrayList<String> attachedVims = WrapperBay.getInstance().getAttachedVims(wimWrapperConfig.getUuid());
            if (attachedVims != null) {
                wimWrapperConfig.setAttachedVims(attachedVims);
            }
            WimApiConfiguration wimApiConfig = getWimApiFromWrapperApi(wimWrapperConfig);

            WimApiConfiguration wimApiUpdateConfig = mapper.readValue(updateWim, WimApiConfiguration.class);

            Logger.info("Update VIM information");
            updateWimApi(wimApiConfig,wimApiUpdateConfig);


            Logger.debug("Update WIM in the DB.");
            wimWrapperConfig = getWimWrapperFromVimApi(wimApiConfig);
            wimWrapperConfig.setWimVendor(WimVendor.getByName(type));


            //Get Vims address, and check if exist
            ArrayList<VimWrapperConfiguration> vims = new ArrayList<>();
            if ((!wimUuid.equals(wimWrapperConfig.getUuid())) || (attachedVims != wimWrapperConfig.getAttachedVims())) {
                VimWrapperConfiguration vimComputeWrapperConfig;
                String wimUuidDb;
                if (wimWrapperConfig.getAttachedVims() != null) {
                    for (String vimUuid : wimWrapperConfig.getAttachedVims()) {
                        wimUuidDb = WrapperBay.getInstance().getWimRepo().getWimUuidFromVimUuid(vimUuid);
                        if ((wimUuidDb != null) && !wimUuidDb.equals(wimUuid)) {
                            Logger.error("VIM " + vimUuid + " already attached to WIM " + wimUuidDb);
                            String body = "{\"status\":\"ERROR\",\"message\":\"VIM " + vimUuid + " already attached to WIM " + wimUuidDb + "\"}";
                            apiResponse = Response.ok((String) body);
                            apiResponse.header("Content-Length", body.length());
                            return apiResponse.status(405).build();
                        }

                        vimComputeWrapperConfig = WrapperBay.getInstance().getConfig(vimUuid);
                        if (vimComputeWrapperConfig == null) {
                            Logger.error("VIM " + vimUuid + " not exist");
                            String body = "{\"status\":\"ERROR\",\"message\":\"VIM " + vimUuid + " not exist\"}";
                            apiResponse = Response.ok((String) body);
                            apiResponse.header("Content-Length", body.length());
                            return apiResponse.status(405).build();
                        }
                        vims.add(vimComputeWrapperConfig);
                    }
                }

            }

            // If update the uuid or the vim list, needs to delete and insert in the attached_vim
            if (!wimUuid.equals(wimWrapperConfig.getUuid())) {
                WrapperBay.getInstance().getWimRepo().removeWimVimLink(wimUuid);
                WrapperBay.getInstance().getWimRepo().updateWimEntry(wimUuid, wimWrapperConfig);

                //Attach Vims to the Wim registered
                if (wimWrapperConfig.getAttachedVims() != null) {
                    for (VimWrapperConfiguration vim : vims) {
                        WrapperBay.getInstance().attachVim(wimWrapperConfig.getUuid(), vim.getUuid(), vim.getVimEndpoint());
                    }
                }

            } else if (attachedVims != wimWrapperConfig.getAttachedVims()) {
                WrapperBay.getInstance().getWimRepo().updateWimEntry(wimUuid, wimWrapperConfig);

                //Attach Vims to the Wim registered
                WrapperBay.getInstance().getWimRepo().removeWimVimLink(wimUuid);
                if (wimWrapperConfig.getAttachedVims() != null) {
                    for (VimWrapperConfiguration vim : vims) {
                        WrapperBay.getInstance().attachVim(wimWrapperConfig.getUuid(), vim.getUuid(), vim.getVimEndpoint());
                    }
                }

            } else {
                WrapperBay.getInstance().getWimRepo().updateWimEntry(wimUuid, wimWrapperConfig);
            }

            Logger.info("Retrieving updated WIM");
            wimWrapperConfig = WrapperBay.getInstance().getWimConfigFromWimUuid(wimWrapperConfig.getUuid());
            if (wimWrapperConfig != null) {
                attachedVims = WrapperBay.getInstance().getAttachedVims(wimWrapperConfig.getUuid());
            } else {
                attachedVims = null;
            }

            if ((wimWrapperConfig == null) || (attachedVims == null)) {
                Logger.error("WIM register failed");
                String body = "{\"status\":\"ERROR\",\"message\":\"WIM register failed\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(400).build();
            }
            wimWrapperConfig.setAttachedVims(attachedVims);
            wimApiConfig = getWimApiFromWrapperApi(wimWrapperConfig);
            String body = mapper.writeValueAsString(wimApiConfig);

            Logger.info("Add WIM call completed.");
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(201).build();

        } catch (Exception e) {
            Logger.error("Error adding the wim: " + e.getMessage(), e);
            String body = "{\"status\":\"ERROR\",\"message\":\"Message malformed, or missing fields, need to be json\"}";
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(400).build();
        }

    }

    /**
     * api call in order to delete a specific registered WIM
     */
    @DELETE
    @Path("/{wimUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVim(@PathParam("wimUuid") String wimUuid) {

        Response.ResponseBuilder apiResponse = null;
        try {
            Logger.info("Retrieving WIM");
            WimWrapperConfiguration wimWrapperConfig = WrapperBay.getInstance().getWimConfigFromWimUuid(wimUuid);


            if (wimWrapperConfig == null) {
                Logger.error("Not Found WIM UUID " + wimUuid);
                String body = "{\"status\":\"ERROR\",\"message\":\"Not Found WIM UUID " + wimUuid + "\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(404).build();
            }

            WrapperBay.getInstance().removeWimWrapper(wimUuid);

            Logger.info("Delete WIM call completed.");
            String body = "{\"status\":\"SUCCESS\",\"message\":\"Deleted WIM UUID " + wimUuid + "\"}";
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(200).build();

        } catch (Exception e) {
            Logger.error("Error getting the wim: " + e.getMessage(), e);
            String body = "{\"status\":\"ERROR\",\"message\":\"Not Found WIM\"}";
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(404).build();
        }

    }

    /**
     * Returns a WimWrapperConfiguration translated from the WimApiConfiguration
     *
     * @param wimApiConfig the user data received from api
     * @return WimWrapperConfiguration object translated from the given data
     * @throws Exception if unable to translate
     */
    private WimWrapperConfiguration getWimWrapperFromVimApi(WimApiConfiguration wimApiConfig) throws Exception {
        WimWrapperConfiguration wimWrapperConfig = new WimWrapperConfiguration();

        wimWrapperConfig.setWrapperType("wim");
        if (wimApiConfig.getUuid() != null) {
            wimWrapperConfig.setUuid(wimApiConfig.getUuid());
        }
        if (wimApiConfig.getName() != null) {
            wimWrapperConfig.setName(wimApiConfig.getName());
        }
        if (wimApiConfig.getEndpoint() != null) {
            wimWrapperConfig.setWimEndpoint(wimApiConfig.getEndpoint());
        }
        if (wimApiConfig.getUserName() != null) {
            wimWrapperConfig.setAuthUserName(wimApiConfig.getUserName());
        }
        if (wimApiConfig.getPassword() != null) {
            wimWrapperConfig.setAuthPass(wimApiConfig.getPassword());
        }
        if (wimApiConfig.getAuthKey() != null) {
            wimWrapperConfig.setAuthKey(wimApiConfig.getAuthKey());
        }
        if (wimApiConfig.getVimList() != null) {
            wimWrapperConfig.setAttachedVims(wimApiConfig.getVimList());
        }

        return wimWrapperConfig;
    }

    /**
     * Returns a WimApiConfiguration translated from the WimWrapperConfiguration
     *
     * @param wimWrapperConfig the wrapper data received from the db
     * @return WimApiConfiguration object translated from the given data
     * @throws Exception if unable to translate
     */
    private WimApiConfiguration getWimApiFromWrapperApi(WimWrapperConfiguration wimWrapperConfig) throws Exception {
        WimApiConfiguration wimApiConfig = new WimApiConfiguration();

        if (wimWrapperConfig.getUuid() != null) {
            wimApiConfig.setUuid(wimWrapperConfig.getUuid());
        }
        if (wimWrapperConfig.getWimVendor() != null) {
            wimApiConfig.setType(wimWrapperConfig.getWimVendor().toString());
        }
        if (wimWrapperConfig.getName() != null) {
            wimApiConfig.setName(wimWrapperConfig.getName());
        }
        if (wimWrapperConfig.getWimEndpoint() != null) {
            wimApiConfig.setEndpoint(wimWrapperConfig.getWimEndpoint());
        }
        if (wimWrapperConfig.getAuthUserName() != null) {
            wimApiConfig.setUserName(wimWrapperConfig.getAuthUserName());
        }
        if (wimWrapperConfig.getAuthPass() != null) {
            wimApiConfig.setPassword(wimWrapperConfig.getAuthPass());
        }
        if (wimWrapperConfig.getAuthKey() != null) {
            wimApiConfig.setAuthKey(wimWrapperConfig.getAuthKey());
        }
        if (wimWrapperConfig.getAttachedVims() != null) {
            wimApiConfig.setVimList(wimWrapperConfig.getAttachedVims());
        }


        return wimApiConfig;
    }

    /**
     * Returns a WimApiConfiguration updated from the WimApiConfiguration sends by user
     *
     * @param wimApiConfig the user data stored in DB
     * @param wimUpdateApiConfig the updated user data received from api
     * @return WimApiConfiguration object updated from the given data
     * @throws Exception if unable to update
     */
    private void updateWimApi(WimApiConfiguration wimApiConfig, WimApiConfiguration wimUpdateApiConfig) throws Exception {

        if (wimUpdateApiConfig.getUuid() != null) {
            wimApiConfig.setUuid(wimUpdateApiConfig.getUuid());
        }
        if (wimUpdateApiConfig.getName() != null) {
            wimApiConfig.setName(wimUpdateApiConfig.getName());
        }
        if (wimUpdateApiConfig.getEndpoint() != null) {
            wimApiConfig.setEndpoint(wimUpdateApiConfig.getEndpoint());
        }
        if (wimUpdateApiConfig.getUserName() != null) {
            wimApiConfig.setUserName(wimUpdateApiConfig.getUserName());
        }
        if (wimUpdateApiConfig.getPassword() != null) {
            wimApiConfig.setPassword(wimUpdateApiConfig.getPassword());
        }
        if (wimUpdateApiConfig.getAuthKey() != null) {
            wimApiConfig.setAuthKey(wimUpdateApiConfig.getAuthKey());
        }
        if (wimUpdateApiConfig.getVimList() != null) {
            wimApiConfig.setVimList(wimUpdateApiConfig.getVimList());
        }
        //return vimApiConfig;
    }

}
