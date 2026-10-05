package io.quarkiverse.docling.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.DoclingServeApiConfig;
import ai.docling.serve.api.health.HealthCheckResponse;
import io.quarkiverse.docling.runtime.client.QuarkusDoclingServeApi;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

class DoclingServeApiProviderTests extends RequestResponseLoggingTests {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.docling.devservices.enabled", "false")
            .overrideRuntimeConfigKey(DoclingRuntimeConfig.BASE_URL_KEY, wiremockUrlForConfig())
            .overrideRuntimeConfigKey("quarkus.docling.async-timeout", "90s");

    @Inject
    DoclingServeApi injectedApi;

    @Test
    void builderResolvesToQuarkusImplementation() {
        var api = DoclingServeApi.builder()
                .baseUrl(resolvedWiremockUrl())
                .build();

        assertThat(api)
                .isInstanceOf(QuarkusDoclingServeApi.class)
                .extracting(DoclingServeApi::health)
                .extracting(HealthCheckResponse::getStatus)
                .isEqualTo("ok");
    }

    @Test
    void configRoundTrips() {
        var config = DoclingServeApi.builder()
                .baseUrl(resolvedWiremockUrl())
                .asyncTimeout(Duration.ofSeconds(45))
                .logRequests()
                .config();

        assertThat(config.toBuilder().build().config())
                .isEqualTo(config);
    }

    @Test
    void injectedApiReportsEffectiveConfig() {
        assertThat(injectedApi.config())
                .isNotNull()
                .extracting(
                        DoclingServeApiConfig::baseUrl,
                        DoclingServeApiConfig::asyncPollInterval,
                        DoclingServeApiConfig::asyncTimeout,
                        DoclingServeApiConfig::logRequests)
                .containsExactly(
                        URI.create(resolvedWiremockUrl()),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(90),
                        false);
    }
}
