package io.quarkiverse.docling.runtime.client;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import io.smallrye.mutiny.Uni;

public interface QuarkusDoclingServeConvertApi {
    @Path("/v1/convert/source")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    ConvertDocumentResponse convertSource(ConvertDocumentRequest request, @BeanParam ApiMetadata metadata);

    @Path("/v1/convert/source/async")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    Uni<TaskStatusPollResponse> submitConvertSourceAsync(ConvertDocumentRequest request, @BeanParam ApiMetadata metadata);
}
