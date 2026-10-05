package io.quarkiverse.docling.runtime.config;

import java.util.Optional;
import java.util.function.Predicate;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;

/**
 * Maps the Quarkus-specific {@link DoclingRuntimeConfig} onto the implementation-agnostic
 * {@link DoclingServeApiConfig} used by the Docling Java SPI.
 */
public final class DoclingConfigMapper {
    private DoclingConfigMapper() {
    }

    /**
     * Converts the given runtime configuration into a {@link DoclingServeApiConfig}.
     *
     * @param config the Quarkus runtime configuration
     * @return the equivalent {@link DoclingServeApiConfig}, with every option explicitly set
     * @throws IllegalArgumentException if no base url is configured
     */
    public static DoclingServeApiConfig toApiConfig(DoclingRuntimeConfig config) {
        var baseUrl = Optional.ofNullable(config.baseUrl())
                .filter(Predicate.not(String::isBlank))
                .orElseThrow(() -> new IllegalArgumentException(
                        DoclingRuntimeConfig.BASE_URL_KEY + " cannot be null or empty"));

        var builder = DoclingServeApi.builder()
                .baseUrl(baseUrl)
                .logRequests(config.logRequests())
                .logResponses(config.logResponses())
                .prettyPrint(config.prettyPrint())
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .asyncPollInterval(config.asyncPollInterval())
                .asyncTimeout(config.asyncTimeout());

        config.apiKey().ifPresent(builder::apiKey);

        // config() only snapshots the options - unlike build(), it does not resolve a provider
        return builder.config();
    }
}
