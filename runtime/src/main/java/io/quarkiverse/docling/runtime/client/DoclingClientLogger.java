package io.quarkiverse.docling.runtime.client;

import static java.util.stream.Collectors.joining;
import static java.util.stream.StreamSupport.stream;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.ClientLogger;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public final class DoclingClientLogger implements ClientLogger {
    private static final Logger LOG = Logger.getLogger(DoclingClientLogger.class);

    private final boolean logRequests;
    private final boolean logResponses;

    public DoclingClientLogger(boolean logRequests, boolean logResponses) {
        this.logRequests = logRequests;
        this.logResponses = logResponses;
    }

    @Override
    public void setBodySize(int bodySize) {
        // ignore
    }

    @Override
    public void logRequest(HttpClientRequest request, Buffer body, boolean omitBody) {
        if (logRequests && LOG.isInfoEnabled()) {
            try {
                LOG.infof("Request:\n- method: %s\n- url: %s\n- headers: %s\n- body: %s", request.getMethod(),
                        request.absoluteURI(), inOneLine(request.headers()), bodyToString(body));
            } catch (Exception e) {
                LOG.warn("Failed to log request", e);
            }
        }
    }

    @Override
    public void logResponse(HttpClientResponse response, boolean redirect) {
        if (logResponses && LOG.isInfoEnabled()) {
            response.bodyHandler(new Handler<>() {
                @Override
                public void handle(Buffer body) {
                    try {
                        LOG.infof("Response:\n- status code: %s\n- headers: %s\n- body: %s", response.statusCode(),
                                inOneLine(response.headers()), bodyToString(body));
                    } catch (Exception e) {
                        LOG.warn("Failed to log response", e);
                    }
                }
            });
        }
    }

    private String bodyToString(Buffer body) {
        return (body != null) ? body.toString() : "";
    }

    private String inOneLine(MultiMap headers) {
        return stream(headers.spliterator(), false)
                .map(header -> "[%s: %s]".formatted(header.getKey(), header.getValue()))
                .collect(joining(", "));
    }
}
