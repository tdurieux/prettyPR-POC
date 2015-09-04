package com.github.tdurieux.prettyPR.api.v0;

import com.github.tdurieux.prettyPR.diff.PrettyPR;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.Set;

@Path("/")
public class PRDiffAPI {

    @GET
    @Path("{user}/{project}/{pr}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response analysePR(@PathParam("user") String user, @PathParam("project") String project, @PathParam("pr") int pr) {
        try {
            final PrettyPR prettyPR = new PrettyPR(user, project, pr);
            prettyPR.run();

            String content = "";

            final Set<String> fileNames = prettyPR.getSemanticPatch().keySet();
            for (Iterator<String> iterator = fileNames.iterator(); iterator.hasNext(); ) {
                String fileName = iterator.next();
                final String patch = prettyPR.getSemanticPatch().get(fileName);
                content += fileName + "\n";
                content += patch + "\n";
            }
            return Response.status(201).entity(content).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("{'error': '" + e.getMessage() + "'}").build();
        }
    }
}
