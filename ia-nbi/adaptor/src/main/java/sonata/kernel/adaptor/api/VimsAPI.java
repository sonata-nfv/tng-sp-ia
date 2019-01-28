package sonata.kernel.adaptor.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import sonata.kernel.adaptor.commons.SonataManifestMapper;
import sonata.kernel.adaptor.wrapper.VimWrapperConfiguration;
import sonata.kernel.adaptor.wrapper.WrapperBay;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

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

            for (String vimUuid : vimUuids) {
                VimWrapperConfiguration vim = WrapperBay.getInstance().getConfig(vimUuid);
                if (vim != null) {
                    output.add(vim);
                }
            }
            ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
            String body = mapper.writeValueAsString(output);

            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(200).build();

        } catch (Exception e) {
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
    @Path("/{vim_uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVim(@PathParam("vim_uuid") String vim_uuid) {

        Response.ResponseBuilder apiResponse = null;
        try {

            VimWrapperConfiguration vim = WrapperBay.getInstance().getConfig(vim_uuid);

            if (vim == null) {
                String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM UUID " + vim_uuid + "\"}";
                apiResponse = Response.ok((String) body);
                apiResponse.header("Content-Length", body.length());
                return apiResponse.status(404).build();
            }

            ObjectMapper mapper = SonataManifestMapper.getSonataJsonMapper();
            String body = mapper.writeValueAsString(vim);

            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(200).build();

        } catch (Exception e) {
            String body = "{\"status\":\"ERROR\",\"message\":\"Not Found VIM\"}";
            apiResponse = Response.ok((String) body);
            apiResponse.header("Content-Length", body.length());
            return apiResponse.status(404).build();
        }

    }
}
