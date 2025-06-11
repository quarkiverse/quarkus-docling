package io.quarkiverse.docling.deployment.config;

import static io.quarkus.runtime.annotations.ConfigPhase.BUILD_TIME;

import io.quarkiverse.docling.deployment.devservices.config.DoclingDevServicesConfig;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigRoot(phase = BUILD_TIME)
@ConfigMapping(prefix = "quarkus.docling")
public interface DoclingBuildTimeConfig {
    /**
     * Dev services related settings
     */
    DoclingDevServicesConfig devservices();
}
