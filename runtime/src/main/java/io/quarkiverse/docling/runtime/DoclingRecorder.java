package io.quarkiverse.docling.runtime;

import java.util.function.Supplier;

import io.quarkiverse.docling.runtime.client.DoclingClientBuilder;
import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DoclingRecorder {
    private final RuntimeValue<DoclingRuntimeConfig> config;

    public DoclingRecorder(RuntimeValue<DoclingRuntimeConfig> config) {
        this.config = config;
    }

    public Supplier<DoclingApi> doclingClient() {
        return new Supplier<DoclingApi>() {
            @Override
            public DoclingApi get() {
                return new DoclingClientBuilder(config.getValue()).build();
            }
        };
    }
}
