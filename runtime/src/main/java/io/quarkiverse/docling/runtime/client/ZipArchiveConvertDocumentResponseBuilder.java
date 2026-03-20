package io.quarkiverse.docling.runtime.client;

import java.io.InputStream;
import java.util.Optional;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import ai.docling.serve.api.convert.response.ZipArchiveConvertDocumentResponse;
import ai.docling.serve.api.util.Utils;

final class ZipArchiveConvertDocumentResponseBuilder {
    private static final String FILENAME_DISPOSITION = "filename=";

    private ZipArchiveConvertDocumentResponseBuilder() {
    }

    static ZipArchiveConvertDocumentResponse build(Response response) {
        var builder = ZipArchiveConvertDocumentResponse.builder();
        getContentDispositionFilename(response).ifPresent(builder::fileName);

        if (response.hasEntity()) {
            builder.inputStream(response.readEntity(InputStream.class));
        }

        return builder.build();
    }

    private static Optional<String> getContentDispositionFilename(Response response) {
        return getHeaderValue(response, HttpHeaders.CONTENT_DISPOSITION)
                .map(contentDisposition -> {
                    var filenameIndex = contentDisposition.indexOf(FILENAME_DISPOSITION);

                    return (filenameIndex >= 0) ? contentDisposition.substring(filenameIndex + FILENAME_DISPOSITION.length())
                            .replaceAll("^\"|\"$", "").trim() : null;
                })
                .filter(Utils::isNotNullOrBlank);
    }

    private static Optional<String> getHeaderValue(Response response, String headerName) {
        return Optional.ofNullable(response.getHeaderString(headerName))
                .filter(Utils::isNotNullOrBlank);
    }
}
