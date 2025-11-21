package io.quarkiverse.docling.deployment.devservices.config;

import java.time.Duration;
import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;

import io.smallrye.config.WithDefault;

import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

@ConfigGroup
public interface DoclingDevServicesConfig extends DoclingServeContainerConfig {
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

    @Override
    default Builder toBuilder() {
        throw new UnsupportedOperationException("This operation is not supported by the DoclingDevServicesConfig");
    }
}
