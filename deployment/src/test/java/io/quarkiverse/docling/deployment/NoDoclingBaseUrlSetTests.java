package io.quarkiverse.docling.deployment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkus.test.QuarkusUnitTest;

class NoDoclingBaseUrlSetTests {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.docling.devservices.enabled", "false");

    @Inject
    DoclingApi DoclingApi;

    @Test
    void test() {
        assertThatThrownBy(() -> DoclingApi.healthHealthGet())
                .hasMessageContaining("quarkus.docling.base-url cannot be null or empty")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
