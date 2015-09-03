package com.github.tdurieux.prettyPR.api.v0;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by thomas on 3/09/15.
 */
@Path("/entry-point")
public class EntryPoint {

    @GET
    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "Test";
    }
}
