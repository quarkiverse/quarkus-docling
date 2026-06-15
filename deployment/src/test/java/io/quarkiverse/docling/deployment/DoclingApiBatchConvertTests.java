package io.quarkiverse.docling.deployment;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.convert.request.BatchConvertDocumentRequest;
import ai.docling.serve.api.convert.request.CallbackSpec;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.request.target.PresignedUrlTarget;
import ai.docling.serve.api.task.response.TaskStatus;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

class DoclingApiBatchConvertTests extends RequestResponseLoggingTests {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.docling.devservices.enabled", "false")
            .overrideRuntimeConfigKey(DoclingRuntimeConfig.BASE_URL_KEY, wiremockUrlForConfig());

    @Inject
    DoclingServeApi doclingApi;

    @Test
    void shouldConvertSourceBatch() {
        wiremock().register(
                post(urlPathEqualTo("/v1/convert/source/batch"))
                        .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON))
                        .willReturn(okJson("""
                                {
                                  "task_id": "batch-task-123",
                                  "task_type": "convert",
                                  "task_status": "pending",
                                  "task_position": 1,
                                  "task_meta": null
                                }
                                """)));

        var request = BatchConvertDocumentRequest.builder()
                .source(HttpSource.builder()
                        .url(URI.create("https://arxiv.org/pdf/2408.09869"))
                        .build())
                .source(HttpSource.builder()
                        .url(URI.create("https://arxiv.org/pdf/2501.17887"))
                        .build())
                .target(PresignedUrlTarget.builder().build())
                .build();

        var response = doclingApi.convertSourceBatch(request);

        assertThat(response).isNotNull();
        assertThat(response.getTaskId()).isEqualTo("batch-task-123");
        assertThat(response.getTaskStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void shouldConvertSourceBatchWithCallbacks() {
        wiremock().register(
                post(urlPathEqualTo("/v1/convert/source/batch"))
                        .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON))
                        .willReturn(okJson("""
                                {
                                  "task_id": "batch-callback-789",
                                  "task_type": "convert",
                                  "task_status": "pending",
                                  "task_position": 1,
                                  "task_meta": null
                                }
                                """)));

        var request = BatchConvertDocumentRequest.builder()
                .source(HttpSource.builder()
                        .url(URI.create("https://arxiv.org/pdf/2408.09869"))
                        .build())
                .target(PresignedUrlTarget.builder().build())
                .callback(CallbackSpec.builder()
                        .url(URI.create("https://my-app.example.com/docling/progress"))
                        .header("Authorization", "Bearer token123")
                        .build())
                .build();

        var response = doclingApi.convertSourceBatch(request);

        assertThat(response).isNotNull();
        assertThat(response.getTaskId()).isEqualTo("batch-callback-789");
        assertThat(response.getTaskStatus()).isEqualTo(TaskStatus.PENDING);
    }
}
