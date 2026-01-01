package io.quarkiverse.docling.deployment.devservices;

import java.util.Collections;
import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item used to carry running DevService values to Dev UI.
 */
public final class DoclingDevServicesConfigBuildItem extends MultiBuildItem {
    private final Map<String, String> config;

    public DoclingDevServicesConfigBuildItem(Map<String, String> config) {
        this.config = (config != null) ? Collections.unmodifiableMap(config) : Map.of();
    }

    public Map<String, String> getConfig() {
        return config;
    }
}
