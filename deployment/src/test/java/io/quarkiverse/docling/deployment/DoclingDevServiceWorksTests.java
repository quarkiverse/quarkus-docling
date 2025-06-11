package io.quarkiverse.docling.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkiverse.docling.runtime.client.model.HealthCheckResponse;

class DoclingDevServiceWorksTests {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideRuntimeConfigKey("quarkus.docling.log-requests", "true")
            .overrideRuntimeConfigKey("quarkus.docling.log-responses", "true");

    @Inject
    DoclingApi DoclingApi;

    @Test
    void hello() {
        assertThat(DoclingApi.healthHealthGet())
                .isNotNull()
                .extracting(HealthCheckResponse::getStatus)
                .isEqualTo("ok");
    }

    // @TODO This is where we need to add some tests for other operations...
}
