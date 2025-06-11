package io.quarkiverse.docling.runtime.config;

import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.time.Duration;

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
}
