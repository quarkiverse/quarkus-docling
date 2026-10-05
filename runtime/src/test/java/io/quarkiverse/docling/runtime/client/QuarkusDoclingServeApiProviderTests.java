package io.quarkiverse.docling.runtime.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

import ai.docling.serve.api.DoclingServeApiConfig;
import ai.docling.serve.api.spi.DoclingServeApiProvider.Unsupported;

class QuarkusDoclingServeApiProviderTests {
    @Test
    void onlyAsyncExecutorIsUnsupported() {
        assertThat(new QuarkusDoclingServeApiProvider().unsupportedOptions())
                .containsExactly(entry(DoclingServeApiConfig.ASYNC_EXECUTOR, Unsupported.WARN));
    }
}
