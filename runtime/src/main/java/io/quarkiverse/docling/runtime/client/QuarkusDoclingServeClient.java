package io.quarkiverse.docling.runtime.client;

import jakarta.ws.rs.Path;

/**
 * A Quarkus REST Client interface for interacting with the Docling Serve API.
 * This client extends the DoclingServeApi interface and includes endpoints for health checks
 * and document conversion from a source.
 *
 * This interface uses JAX-RS annotations to define the resource paths, HTTP methods, and media types
 * for requests and responses.
 */
@Path("/")
public interface QuarkusDoclingServeClient
        extends QuarkusDoclingServeHealthApi, QuarkusDoclingServeTaskApi, QuarkusDoclingServeConvertApi,
        QuarkusDoclingServeClearApi, QuarkusDoclingServeChunkApi {

}
