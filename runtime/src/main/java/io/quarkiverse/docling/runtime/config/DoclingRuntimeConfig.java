package io.quarkiverse.docling.runtime.config;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot(phase = RUN_TIME)
@ConfigMapping(prefix = "quarkus.docling")
public interface DoclingRuntimeConfig {
    /**
     * The key for the base url
     */
    String BASE_URL_KEY = "quarkus.docling.base-url";

    /**
     * The configuration key used to set the API key for authenticating requests in the Docling client.
     */
    String API_KEY_KEY = "quarkus.docling.api-key";

    /**
     * The default base url of where docling is
     */
    String baseUrl();

    /**
     * Timeout for Docling calls
     */
    @WithDefault("1m")
    Duration timeout();

    /**
     * Whether the Docling client should log requests
     */
    @WithDefault("false")
    Boolean logRequests();

    /**
     * Whether the Docling client should log responses
     */
    @WithDefault("false")
    Boolean logResponses();

    /**
     * Controls whether request/response bodies are pretty-printed if
     * {@link #logRequests()} or {@link #logResponses()} is set to {@code true}
     */
    @WithDefault("false")
    Boolean prettyPrint();

    /**
     * Sets the polling interval for async operations.
     */
    @WithDefault("2s")
    Duration asyncPollInterval();

    /**
     * Sets the timeout for async operations.
     */
    @WithDefault("${quarkus.docling.timeout:5m}")
    Duration asyncTimeout();

    /**
     * Sets the API key for authenticating requests made by the client being built.
     *
     * The provided API key will be used as a credential to authorize and authenticate
     * API requests. This method updates the configuration of the builder with the specified
     * API key and ensures that the API client includes it in its requests as required for
     * secure access to the API.
     */
    Optional<String> apiKey();
}
