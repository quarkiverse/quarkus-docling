package io.quarkiverse.docling.runtime.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.reactive.client.api.LoggingScope;

import ai.docling.serve.api.DoclingServeApiConfig;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

public final class DoclingClientBuilder {
    private String baseUrl;
    private Duration connectTimeout;
    private Duration readTimeout;
    private boolean logRequests;
    private boolean logResponses;
    private boolean prettyPrint;

    public DoclingClientBuilder(DoclingServeApiConfig config) {
        baseUrl(config.baseUrl().toString());
        logRequests(config.logRequests());
        logResponses(config.logResponses());
        prettyPrint(config.prettyPrint());
        connectTimeout(config.connectTimeout());
        readTimeout(config.readTimeout());
    }

    public DoclingClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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

    public DoclingClientBuilder prettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        return this;
    }

    public DoclingClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public DoclingClientBuilder readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public QuarkusDoclingServeClient build() {
        if ((this.baseUrl == null) || this.baseUrl.trim().isBlank()) {
            throw new IllegalArgumentException(DoclingRuntimeConfig.BASE_URL_KEY + " cannot be null or empty");
        }

        try {
            var restApiBuilder = QuarkusRestClientBuilder.newBuilder()
                    .baseUri(new URI(this.baseUrl))
                    .connectTimeout(this.connectTimeout.toSeconds(), TimeUnit.SECONDS)
                    .readTimeout(this.readTimeout.toSeconds(), TimeUnit.SECONDS);

            if (this.logRequests || this.logResponses) {
                restApiBuilder
                        .loggingScope(LoggingScope.REQUEST_RESPONSE)
                        .clientLogger(new DoclingClientLogger(this.logRequests, this.logResponses, this.prettyPrint));
            }

            return restApiBuilder.build(QuarkusDoclingServeClient.class);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
