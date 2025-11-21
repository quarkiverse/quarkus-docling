package io.quarkiverse.docling.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ai.docling.api.serve.DoclingServeApi;
import ai.docling.api.serve.health.HealthCheckResponse;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

class DoclingApiLogRequestsResponsesTests extends RequestResponseLoggingTests {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.docling.devservices.enabled", "false")
            .overrideRuntimeConfigKey(DoclingRuntimeConfig.BASE_URL_KEY, wiremockUrlForConfig())
            .overrideRuntimeConfigKey("quarkus.docling.log-requests", "true")
            .overrideRuntimeConfigKey("quarkus.docling.log-responses", "true");

    @Inject
    DoclingServeApi doclingApi;

    @Test
    void requestResponseLogged() {
        assertThat(doclingApi.health())
                .isNotNull()
                .extracting(HealthCheckResponse::getStatus)
                .isEqualTo("ok");

        assertThat(LOG_HANDLER.getRecords())
                .hasSize(2)
                .satisfies(
                        logRecord -> assertThat(logRecord)
                                .isNotNull()
                                .extracting(
                                        LogRecord::getLevel,
                                        l -> Objects.toString(l.getParameters()[0]),
                                        l -> l.getParameters()[1])
                                .containsExactly(
                                        Level.INFO,
                                        HttpMethod.GET,
                                        resolvedWiremockUrl("/health")),
                        atIndex(0))
                .satisfies(
                        logRecord -> assertThat(logRecord)
                                .isNotNull()
                                .extracting(
                                        LogRecord::getLevel,
                                        l -> l.getParameters()[0])
                                .containsExactly(
                                        Level.INFO,
                                        Status.OK.getStatusCode()),
                        atIndex(1));
    }
}
