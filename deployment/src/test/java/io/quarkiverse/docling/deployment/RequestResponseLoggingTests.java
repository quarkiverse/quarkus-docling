package io.quarkiverse.docling.deployment;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.logging.LogManager;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;

import io.quarkiverse.docling.runtime.client.DoclingClientLogger;
import io.quarkiverse.docling.testing.internal.WiremockAware;
import io.quarkus.test.InMemoryLogHandler;

abstract class RequestResponseLoggingTests extends WiremockAware {
    static final InMemoryLogHandler LOG_HANDLER = new InMemoryLogHandler(logRecord -> true);

    static {
        LogManager.getLogManager().getLogger(DoclingClientLogger.class.getName()).addHandler(LOG_HANDLER);
    }

    @BeforeEach
    void beforeEach() {
        wiremock().register(
                get(urlPathEqualTo("/health"))
                        .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
                        .willReturn(okJson("""
                                {
                                  "status": "ok"
                                }
                                """)));
    }
}
