package io.quarkiverse.docling.runtime.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ai.docling.serve.api.DoclingServeApiConfig;

class DoclingConfigMapperTests {
    private static DoclingRuntimeConfig fullyConfigured() {
        var config = mock(DoclingRuntimeConfig.class);
        when(config.baseUrl()).thenReturn("http://localhost:5001");
        when(config.apiKey()).thenReturn(Optional.of("some-api-key"));
        when(config.logRequests()).thenReturn(true);
        when(config.logResponses()).thenReturn(true);
        when(config.prettyPrint()).thenReturn(false);
        when(config.connectTimeout()).thenReturn(Duration.ofSeconds(5));
        when(config.readTimeout()).thenReturn(Duration.ofSeconds(30));
        when(config.asyncPollInterval()).thenReturn(Duration.ofSeconds(2));
        when(config.asyncTimeout()).thenReturn(Duration.ofMinutes(5));

        return config;
    }

    @Test
    void mapsEveryOption() {
        assertThat(DoclingConfigMapper.toApiConfig(fullyConfigured()))
                .isNotNull()
                .extracting(
                        DoclingServeApiConfig::baseUrl,
                        DoclingServeApiConfig::apiKey,
                        DoclingServeApiConfig::logRequests,
                        DoclingServeApiConfig::logResponses,
                        DoclingServeApiConfig::prettyPrint,
                        DoclingServeApiConfig::connectTimeout,
                        DoclingServeApiConfig::readTimeout,
                        DoclingServeApiConfig::asyncPollInterval,
                        DoclingServeApiConfig::asyncTimeout,
                        DoclingServeApiConfig::asyncExecutor)
                .containsExactly(
                        URI.create("http://localhost:5001"),
                        "some-api-key",
                        true,
                        true,
                        false,
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(2),
                        Duration.ofMinutes(5),
                        null);
    }

    @Test
    void unsetApiKeyIsNotExplicitlySet() {
        var config = fullyConfigured();
        when(config.apiKey()).thenReturn(Optional.empty());

        assertThat(DoclingConfigMapper.toApiConfig(config))
                .returns(null, DoclingServeApiConfig::apiKey)
                .returns(false, c -> c.isExplicitlySet(DoclingServeApiConfig.API_KEY))
                .returns(true, c -> c.isExplicitlySet(DoclingServeApiConfig.BASE_URL));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   " })
    void baseUrlIsRequired(String baseUrl) {
        var config = fullyConfigured();
        when(config.baseUrl()).thenReturn(baseUrl);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> DoclingConfigMapper.toApiConfig(config))
                .withMessage(DoclingRuntimeConfig.BASE_URL_KEY + " cannot be null or empty");
    }
}
