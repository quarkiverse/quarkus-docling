package io.quarkiverse.docling.deployment.devservices.config;

import java.time.Duration;
import java.util.Map;

import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DoclingDevServicesConfig extends DoclingServeContainerConfig {
    /**
     * The default API key used for authentication or integration with external services when no other
     * key is configured or provided. This serves as a placeholder or fallback value.
     */
    String DEFAULT_API_KEY = "default-api-key";

    /**
     * If DevServices has been explicitly enabled or disabled. DevServices are generally enabled
     * by default, unless there is an existing configuration present.
     * <p>
     * When DevServices is enabled, Quarkus will attempt to automatically configure and start a
     * Docling server when running in Dev or Test mode.
     * </p>
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The container image name to use.
     */
    @WithDefault(DOCLING_IMAGE)
    @Override
    String image();

    /**
     * Whether or not to enable the docling UI
     * <p>
     * See https://github.com/docling-project/docling-serve?tab=readme-ov-file#demonstration-ui
     * </p>
     */
    @WithDefault("true")
    @Override
    boolean enableUi();

    /**
     * Environment variables that are passed to the container
     */
    @Override
    Map<String, String> containerEnv();

    /**
     * Specifies the maximum duration to wait for the container to fully start up.
     * This value determines the timeout period for the startup process, ensuring that the container is operational within a
     * defined timeframe.
     * Default Value: {@code quarkus.devservices.timeout}
     */
    @Override
    @WithDefault("${quarkus.devservices.timeout:PT1M}")
    Duration startupTimeout();

    /**
     * Retrieves the API key used for authentication or integration with external services.
     *
     * @return the API key as a {@link String}, or {@code null} if no API key is configured.
     */
    @Override
    @WithDefault(DEFAULT_API_KEY)
    String apiKey();

    /**
     * Indicates if the Docling server managed by Quarkus Dev Services is shared.
     * When shared, Quarkus looks for running containers using label-based service discovery.
     * If a matching container is found, it is used, and so a second one is not started.
     * Otherwise, Dev Services for Docling starts a new container.
     * <p>
     * The discovery uses the {@code quarkus-dev-service-docling} label.
     * The value is configured using the {@code service-name} property.
     * <p>
     * Container sharing is only used in dev mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * The value of the {@code quarkus-dev-service-docling} label attached to the started container.
     * This property is used when {@code shared} is set to {@code true}.
     * In this case, before starting a container, Dev Services for Docling looks for a container with the
     * {@code quarkus-dev-service-docling} label
     * set to the configured value. If found, it will use this container instead of starting a new one. Otherwise, it
     * starts a new container with the {@code quarkus-dev-service-docling} label set to the specified value.
     * <p>
     * This property is used when you need multiple shared Docling servers.
     */
    @WithDefault("docling")
    String serviceName();

    @Override
    default Builder toBuilder() {
        return null;
        //        throw new UnsupportedOperationException("This operation is not supported by the DoclingDevServicesConfig");
    }
}
