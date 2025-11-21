package io.quarkiverse.docling.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.docling.api.serve.DoclingServeApi;
import io.quarkiverse.docling.runtime.client.DoclingClientBuilder;
import io.quarkiverse.docling.runtime.client.DoclingService;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class DoclingRecorder {
    private final RuntimeValue<DoclingRuntimeConfig> config;

    public DoclingRecorder(RuntimeValue<DoclingRuntimeConfig> config) {
        this.config = config;
    }

    public Supplier<DoclingServeApi> doclingServeApi() {
        return new Supplier<DoclingServeApi>() {
            @Override
            public DoclingServeApi get() {
                return new DoclingClientBuilder(config.getValue()).build();
            }
        };
    }

    public Function<SyntheticCreationalContext<DoclingService>, DoclingService> doclingService() {
        return new Function<SyntheticCreationalContext<DoclingService>, DoclingService>() {
            @Override
            public DoclingService apply(SyntheticCreationalContext<DoclingService> context) {
                return new DoclingService(context.getInjectedReference(DoclingServeApi.class));
            }
        };
    }
}
