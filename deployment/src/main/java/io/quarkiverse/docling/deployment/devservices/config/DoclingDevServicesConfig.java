package io.quarkiverse.docling.deployment.devservices.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface DoclingDevServicesConfig {
    /**
     * Default image name
     */
    String DOCLING_IMAGE = "quay.io/docling-project/docling-serve:main";

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
    String imageName();

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
}
