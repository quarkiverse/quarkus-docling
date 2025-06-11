package io.quarkiverse.docling.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkiverse.docling.runtime.client.model.HealthCheckResponse;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

class DoclingApiLogResponsesTests extends RequestResponseLoggingTests {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.docling.devservices.enabled", "false")
            .overrideRuntimeConfigKey(DoclingRuntimeConfig.BASE_URL_KEY, wiremockUrlForConfig())
            .overrideRuntimeConfigKey("quarkus.docling.log-responses", "true");

    @Inject
    DoclingApi DoclingApi;

    @Test
    void responseLogged() {
        assertThat(DoclingApi.healthHealthGet())
                .isNotNull()
                .extracting(HealthCheckResponse::getStatus)
                .isEqualTo("ok");

        assertThat(LOG_HANDLER.getRecords())
                .singleElement()
                .extracting(
                        LogRecord::getLevel,
                        l -> l.getParameters()[0])
                .containsExactly(
                        Level.INFO,
                        Status.OK.getStatusCode());
    }
}
