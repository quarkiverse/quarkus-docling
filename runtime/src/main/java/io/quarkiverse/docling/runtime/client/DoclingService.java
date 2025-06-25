package io.quarkiverse.docling.runtime.client;

import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkiverse.docling.runtime.client.model.ConversionRequest;
import io.quarkiverse.docling.runtime.client.model.ConvertDocumentResponse;
import io.quarkiverse.docling.runtime.client.model.ConvertDocumentsOptions;
import io.quarkiverse.docling.runtime.client.model.FileSource;
import io.quarkiverse.docling.runtime.client.model.HttpSource;
import io.quarkiverse.docling.runtime.client.model.OutputFormat;

/**
 * Service class for interacting with the Docling API. Provides methods for document conversion
 * and health status checks of the external API service.
 */
public class DoclingService {
    private final DoclingApi doclingApi;

    /**
     * Enumerates the possible statuses with an optional associated string representation.
     */
    public enum Status {
        OK("ok"),
        NOT_OK;

        private final String statusText;

        Status(String statusText) {
            this.statusText = statusText;
        }

        Status() {
            this(null);
        }

        public static Optional<Status> from(String statusText) {
            return Optional.ofNullable(statusText)
                    .flatMap(st -> Arrays.stream(values())
                            .filter(status -> st.equals(status.statusText))
                            .findFirst());
        }
    }

    public DoclingService(DoclingApi doclingApi) {
        this.doclingApi = doclingApi;
    }

    /**
     * Converts a document from a URI to given output format.
     *
     * @param uri of document to convert. It should be reachable from docling server.
     * @param outputFormat of the parsed document.
     * @return The response from docling serve.
     */
    public ConvertDocumentResponse convertFromUri(URI uri, OutputFormat outputFormat) {
        HttpSource httpSource = new HttpSource();
        httpSource.setUrl(uri);

        ConversionRequest conversionRequest = new ConversionRequest();
        conversionRequest.addHttpSourcesItem(httpSource);

        ConvertDocumentsOptions convertDocumentsOptions = new ConvertDocumentsOptions();
        convertDocumentsOptions.setToFormats(List.of(outputFormat));
        conversionRequest.options(convertDocumentsOptions);

        return this.doclingApi
                .processUrlV1alphaConvertSourcePost(conversionRequest);
    }

    /**
     * Converts a document from a byte[] to given output format.
     *
     * @param content as chunk of bytes
     * @param filename of input. Used for detecting input format.
     * @param outputFormat of the parsed document.
     * @return The response from docling serve.
     */
    public ConvertDocumentResponse convertFromBytes(
            byte[] content, String filename, OutputFormat outputFormat) {
        String base64Document = Base64.getEncoder().encodeToString(content);

        return this.convertFromBase64(base64Document, filename, outputFormat);
    }

    /**
     * Converts a document from a Base64 string to given output format.
     *
     * @param base64Content
     * @param filename of input. Used for detecting input format.
     * @param outputFormat of the parsed document.
     * @return The response from docling serve.
     */
    public ConvertDocumentResponse convertFromBase64(
            String base64Content, String filename, OutputFormat outputFormat) {

        FileSource fileSource = new FileSource();
        fileSource.base64String(base64Content);
        fileSource.setFilename(filename);

        ConversionRequest conversionRequest = new ConversionRequest();
        conversionRequest.addFileSourcesItem(fileSource);
        ConvertDocumentsOptions convertDocumentsOptions = new ConvertDocumentsOptions();
        convertDocumentsOptions.setToFormats(List.of(outputFormat));
        conversionRequest.options(convertDocumentsOptions);

        return this.doclingApi
                .processUrlV1alphaConvertSourcePost(conversionRequest);
    }

    /**
     * Checks the health status of the external Docling API service.
     *
     * @return {@code true} if the health status returned from the service is "ok"; {@code false} otherwise.
     */
    public boolean isHealthy() {
        return Status.from(this.doclingApi.healthHealthGet().getStatus())
                .map(status -> status == Status.OK)
                .orElse(false);
    }
}
