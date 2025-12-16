package io.quarkiverse.docling.runtime.client;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.docling.serve.api.health.HealthCheckResponse;

public interface QuarkusDoclingServeHealthApi {
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    HealthCheckResponse health(@BeanParam ApiMetadata metadata);
}
