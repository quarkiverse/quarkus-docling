package io.quarkiverse.docling.deployment.devservices.config;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DoclingDevServicesConfig {
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
    @WithDefault(DoclingServeContainerConfig.DOCLING_IMAGE)
    String image();

    /**
     * Whether or not to enable the docling UI
     * <p>
     * See https://github.com/docling-project/docling-serve?tab=readme-ov-file#demonstration-ui
     * </p>
     */
    @WithDefault("true")
    boolean enableUi();

    /**
     * Environment variables that are passed to the container
     */
    Map<String, String> containerEnv();

    /**
     * Specifies the maximum duration to wait for the container to fully start up.
     * This value determines the timeout period for the startup process, ensuring that the container is operational within a
     * defined timeframe.
     * Default Value: {@code quarkus.devservices.timeout}
     */
    @WithDefault("${quarkus.devservices.timeout:PT1M}")
    Duration startupTimeout();

    /**
     * Retrieves the API key used for authentication or integration with external services.
     *
     * @return the API key as a {@link String}, or {@code null} if no API key is configured.
     */
    Optional<String> apiKey();

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

    /**
     * Converts the current configuration of the {@code DoclingDevServicesConfig} into
     * a {@link DoclingServeContainerConfig} object.
     *
     * The resulting {@link DoclingServeContainerConfig} represents the finalized configuration
     * for the Docling server, including container image, environment variables, UI enablement,
     * startup timeout, and the API key if provided.
     *
     * @return a new instance of {@link DoclingServeContainerConfig} built from the current configuration.
     */
    default DoclingServeContainerConfig toDoclingServeContainerConfig() {
        return DoclingServeContainerConfig.builder()
                .image(image())
                .enableUi(enableUi())
                .containerEnv(containerEnv())
                .startupTimeout(startupTimeout())
                .apiKey(apiKey().orElse(null))
                .build();
    }

    default String asString() {
        return "DoclingDevServicesConfig{" +
                "enabled=" + enabled() +
                ", image='" + image() + '\'' +
                ", enableUi=" + enableUi() +
                ", containerEnv=" + containerEnv() +
                ", startupTimeout=" + startupTimeout() +
                ", apiKey=" + apiKey().orElse(null) +
                ", shared=" + shared() +
                ", serviceName='" + serviceName() + '\'' +
                '}';
    }
}
