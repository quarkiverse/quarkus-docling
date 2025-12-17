package io.quarkiverse.docling.runtime.client;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import ai.docling.serve.api.clear.response.ClearResponse;

public interface QuarkusDoclingServeClearApi {
    @Path("/v1/clear/converters")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClearResponse clearConverters(@BeanParam ApiMetadata metadata);

    @Path("/v1/clear/results")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    ClearResponse clearResults(@QueryParam("older_then") long olderThenSeconds, @BeanParam ApiMetadata metadata);
}
