package io.quarkiverse.docling.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkiverse.docling.runtime.client.model.HealthCheckResponse;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

class DoclingApiLogRequestsTests extends RequestResponseLoggingTests {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.docling.devservices.enabled", "false")
            .overrideRuntimeConfigKey(DoclingRuntimeConfig.BASE_URL_KEY, wiremockUrlForConfig())
            .overrideRuntimeConfigKey("quarkus.docling.log-requests", "true");

    @Inject
    DoclingApi doclingApi;

    @Test
    void requestLogged() {
        assertThat(doclingApi.healthHealthGet())
                .isNotNull()
                .extracting(HealthCheckResponse::getStatus)
                .isEqualTo("ok");

        assertThat(LOG_HANDLER.getRecords())
                .singleElement()
                .extracting(
                        LogRecord::getLevel,
                        l -> Objects.toString(l.getParameters()[0]),
                        l -> l.getParameters()[1])
                .containsExactly(
                        Level.INFO,
                        HttpMethod.GET,
                        resolvedWiremockUrl("/health"));
    }
}
