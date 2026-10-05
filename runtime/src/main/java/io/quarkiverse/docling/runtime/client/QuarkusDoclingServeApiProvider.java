package io.quarkiverse.docling.runtime.client;

import java.util.Map;

import ai.docling.serve.api.ConfigOption;
import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;
import ai.docling.serve.api.spi.DoclingServeApiProvider;

/**
 * The {@link DoclingServeApiProvider} of this extension, creating a {@link QuarkusDoclingServeApi}
 * backed by a Quarkus REST client.
 *
 * <p>
 * Within a Quarkus application the recommended way to obtain a {@link DoclingServeApi} is to inject it,
 * which reuses the container-managed {@link QuarkusDoclingServeClient} configured through
 * {@code quarkus.docling.*}. This provider exists so that {@link DoclingServeApi#builder()} also works,
 * creating a client that is not managed by the container.
 */
public final class QuarkusDoclingServeApiProvider implements DoclingServeApiProvider {
    @Override
    public DoclingServeApi create(DoclingServeApiConfig config) {
        return QuarkusDoclingServeApi.builder()
                .config(config)
                .client(new DoclingClientBuilder(config).build())
                .build();
    }

    @Override
    public Map<ConfigOption<?>, Unsupported> unsupportedOptions() {
        // Async operations always run on the Mutiny default worker pool
        return Map.of(DoclingServeApiConfig.ASYNC_EXECUTOR, Unsupported.WARN);
    }
}
