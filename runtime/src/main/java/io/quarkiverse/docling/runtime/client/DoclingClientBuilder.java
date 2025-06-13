package io.quarkiverse.docling.runtime.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.reactive.client.api.LoggingScope;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;

public final class DoclingClientBuilder {
    private String baseUrl;
    private Duration timeout;
    private boolean logRequests;
    private boolean logResponses;

    public DoclingClientBuilder(DoclingRuntimeConfig config) {
        baseUrl(config.baseUrl());
        timeout(config.timeout());
        logRequests(config.logRequests());
        logResponses(config.logResponses());
    }

    public DoclingClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public DoclingClientBuilder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public DoclingClientBuilder logRequests(boolean logRequests) {
        this.logRequests = logRequests;
        return this;
    }

    public DoclingClientBuilder logResponses(boolean logResponses) {
        this.logResponses = logResponses;
        return this;
    }

    public DoclingApi build() {
        if ((baseUrl == null) || baseUrl.trim().isBlank()) {
            throw new IllegalArgumentException(DoclingRuntimeConfig.BASE_URL_KEY + " cannot be null or empty");
        }

        var defaultTimeout = Optional.ofNullable(this.timeout).orElse(Duration.ofMinutes(1));

        try {
            var restApiBuilder = QuarkusRestClientBuilder.newBuilder()
                    .baseUri(new URI(baseUrl))
                    .connectTimeout(defaultTimeout.toSeconds(), TimeUnit.SECONDS)
                    .readTimeout(defaultTimeout.toSeconds(), TimeUnit.SECONDS);

            if (logRequests || logResponses) {
                restApiBuilder
                        .loggingScope(LoggingScope.REQUEST_RESPONSE)
                        .clientLogger(new DoclingClientLogger(logRequests, logResponses));
            }

            return restApiBuilder.build(DoclingApi.class);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
