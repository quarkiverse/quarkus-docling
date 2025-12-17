package io.quarkiverse.docling.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.docling.serve.api.DoclingServeApi;
import io.quarkiverse.docling.runtime.client.DoclingClientBuilder;
import io.quarkiverse.docling.runtime.client.DoclingService;
import io.quarkiverse.docling.runtime.client.QuarkusDoclingServeApi;
import io.quarkiverse.docling.runtime.client.QuarkusDoclingServeClient;
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

    public Supplier<QuarkusDoclingServeClient> doclingServeClient() {
        return new Supplier<QuarkusDoclingServeClient>() {
            @Override
            public QuarkusDoclingServeClient get() {
                return new DoclingClientBuilder(config.getValue()).build();
            }
        };
    }

    public Function<SyntheticCreationalContext<DoclingServeApi>, DoclingServeApi> doclingServeApi() {
        return new Function<SyntheticCreationalContext<DoclingServeApi>, DoclingServeApi>() {
            @Override
            public DoclingServeApi apply(SyntheticCreationalContext<DoclingServeApi> context) {
                return new QuarkusDoclingServeApi(context.getInjectedReference(QuarkusDoclingServeClient.class),
                        config.getValue());
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
