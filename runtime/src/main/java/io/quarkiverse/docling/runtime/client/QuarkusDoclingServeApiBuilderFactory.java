package io.quarkiverse.docling.runtime.client;

import ai.docling.serve.api.spi.DoclingServeApiBuilderFactory;
import io.quarkiverse.docling.runtime.client.QuarkusDoclingServeApi.QuarkusDoclingServeApiBuilder;

public class QuarkusDoclingServeApiBuilderFactory implements DoclingServeApiBuilderFactory {
    @Override
    public QuarkusDoclingServeApiBuilder getBuilder() {
        return QuarkusDoclingServeApi.builder();
    }
}
