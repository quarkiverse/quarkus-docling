package io.quarkiverse.docling.runtime;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.RuntimeValue;

import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;

class DoclingRecorderTests {
    DoclingRuntimeConfig config = mock(DoclingRuntimeConfig.class);
    DoclingRecorder recorder = new DoclingRecorder(new RuntimeValue<>(config));

    @Test
    void noBaseUrlSet() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> recorder.doclingClient().get())
                .withFailMessage("quarkus.docling.base-url cannot be null or empty");
    }
}
