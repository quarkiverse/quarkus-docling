package io.quarkiverse.docling.runtime.client;

import java.net.URI;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import ai.docling.serve.api.validation.ValidationError;
import ai.docling.serve.api.validation.ValidationException;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;

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

    @ClientExceptionMapper
    static RuntimeException toException(Response response, URI uri) {
        var statusCode = response.getStatus();
        var cause = createException(response);

        if (statusCode == 422) {
            var msg = "An error occurred while making request to %s".formatted(uri.toString());

            if (response.hasEntity()) {
                return new ValidationException(response.readEntity(ValidationError.class), cause, msg);
            } else {
                return new ValidationException(null, cause, msg);
            }
        }

        return cause;
    }

    private static WebApplicationException createException(Response response) {
        var statusCode = response.getStatus();

        if (statusCode >= 500) {
            return new ServerErrorException(response);
        } else if (statusCode >= 400) {
            return new ClientErrorException(response);
        }

        return new WebApplicationException(response);
    }
}
