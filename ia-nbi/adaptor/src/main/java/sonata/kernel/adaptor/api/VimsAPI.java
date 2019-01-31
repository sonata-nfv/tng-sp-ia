package sonata.kernel.adaptor.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import sonata.kernel.adaptor.commons.SonataManifestMapper;
import sonata.kernel.adaptor.wrapper.ComputeVimVendor;
import sonata.kernel.adaptor.wrapper.VimWrapperConfiguration;
import sonata.kernel.adaptor.wrapper.WrapperBay;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.UUID;

@Path("/vims")
public class VimsAPI {

    private static final org.slf4j.Logger Logger = LoggerFactory.getLogger(VimsAPI.class);

    /**
     * api call in order to get a list of the registered compute VIMs
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVims() {

        ArrayList<VimWrapperConfiguration> output = new ArrayList<>();
        Response.ResponseBuilder apiResponse = null;
        try {
            ArrayList<String> vimUuids = WrapperBay.getInstance().getComputeWrapperList();
            Logger.info("Found " + vimUuids.size() + " VIMs");
            Logger.info("Retrieving VIM(s)");
            for (String vimUuid : vimUuids) {
                VimWrapperConfiguration vim = WrapperBay.getInstance().getConfig(vimUuid);
                if (vim != null) {
                    output.add(vim);
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
     * api call in order to get a specific registered compute VIM
     */
    @GET
    @Path("/{vimUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVim(@PathParam("vimUuid") String vimUuid) {

        Response.ResponseBuilder apiResponse = null;
        try {
            Logger.info("Retrieving VIM");
            VimWrapperConfiguration vim = WrapperBay.getInstance().getConfig(vimUuid);

            if (vim == null) {
                String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM UUID " + vimUuid + "\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(404).build();
            }

            ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
            String body = mapper.writeValueAsString(vim);

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
     * api call in order to register a compute VIM
     */
    @POST
    @Path("/{type}")
    //@SuppressWarnings("null")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addVim(@PathParam("type") String type, String newVim) {

        ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
        Response.ResponseBuilder apiResponse = null;
        Logger.debug("VIM Configuration received: ");
        System.out.println(newVim);

        try {
            VimWrapperConfiguration vimConfig = mapper.readValue(newVim, VimWrapperConfiguration.class);
            Logger.info("Try Retrieving VIM");
            VimWrapperConfiguration vim = WrapperBay.getInstance().getConfig(vimConfig.getUuid());

            if (vim != null) {
                String body = "{\"status\":\"ERROR\",\"message\":\"VIM " + vimConfig.getUuid() + " already exist\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(405).build();
            }

            Logger.debug("Registering a COMPUTE wrapper.");
            vimConfig.setVimVendor(ComputeVimVendor.getByName(type));
            if (vimConfig.getUuid() == null) {
                vimConfig.setUuid(UUID.randomUUID().toString());
            }
            if (vimConfig.getDomain() == null) {
                vimConfig.setDomain("Default");
            }

            WrapperBay.getInstance().registerComputeWrapper(vimConfig);

            Logger.info("Retrieving new VIM");
            vim = WrapperBay.getInstance().getConfig(vimConfig.getUuid());
            if (vim == null) {
                String body = "{\"status\":\"ERROR\",\"message\":\"VIM " + vimConfig.getUuid() + " register failed\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(400).build();
            }
            //ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
            String body = mapper.writeValueAsString(vim);

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
     * api call in order to delete a specific registered compute VIM
     */
    @DELETE
    @Path("/{vimUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVim(@PathParam("vimUuid") String vimUuid) {

        Response.ResponseBuilder apiResponse = null;
        try {
            Logger.info("Retrieving VIM");
            VimWrapperConfiguration vim = WrapperBay.getInstance().getConfig(vimUuid);

            if (vim == null) {
                String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM UUID " + vimUuid + "\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(404).build();
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
}
