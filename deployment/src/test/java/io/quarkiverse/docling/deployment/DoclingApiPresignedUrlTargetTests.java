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
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.request.source.HttpSource;
import ai.docling.serve.api.convert.request.target.PresignedUrlTarget;
import ai.docling.serve.api.convert.response.ArtifactType;
import ai.docling.serve.api.convert.response.ConversionStatus;
import ai.docling.serve.api.convert.response.PreSignedUrlConvertResponse;
import ai.docling.serve.api.convert.response.ResponseType;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.test.QuarkusUnitTest;

class DoclingApiPresignedUrlTargetTests extends RequestResponseLoggingTests {
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.docling.devservices.enabled", "false")
            .overrideRuntimeConfigKey(DoclingRuntimeConfig.BASE_URL_KEY, wiremockUrlForConfig());

    @Inject
    DoclingServeApi doclingApi;

    @Test
    void shouldConvertWithPresignedUrlTarget() {
        wiremock().register(
                post(urlPathEqualTo("/v1/convert/source"))
                        .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON))
                        .willReturn(okJson("""
                                {
                                  "processing_time": 4.13,
                                  "num_converted": 1,
                                  "num_succeeded": 1,
                                  "num_partially_succeeded": 0,
                                  "num_failed": 0,
                                  "documents": [
                                    {
                                      "source_index": 0,
                                      "source_uri": "https://arxiv.org/pdf/2408.09869",
                                      "filename": "2408.09869",
                                      "status": "success",
                                      "errors": [],
                                      "timings": {},
                                      "artifacts": [
                                        {
                                          "artifact_type": "markdown",
                                          "mime_type": "text/markdown",
                                          "uri": "https://storage.example.com/2408.09869.md",
                                          "url_expires_at": "2026-06-15T12:00:00Z"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """)));

        var request = ConvertDocumentRequest.builder()
                .source(HttpSource.builder()
                        .url(URI.create("https://arxiv.org/pdf/2408.09869"))
                        .build())
                .target(PresignedUrlTarget.builder().build())
                .build();

        var response = doclingApi.convertSource(request);

        assertThat(response)
                .isNotNull()
                .isInstanceOf(PreSignedUrlConvertResponse.class);

        assertThat(response.getResponseType())
                .isEqualTo(ResponseType.PRE_SIGNED_URL_RESPONSE);

        var presignedResponse = (PreSignedUrlConvertResponse) response;
        assertThat(presignedResponse.getProcessingTime()).isEqualTo(4.13);
        assertThat(presignedResponse.getNumConverted()).isEqualTo(1);
        assertThat(presignedResponse.getNumSucceeded()).isEqualTo(1);
        assertThat(presignedResponse.getNumPartiallySucceeded()).isZero();
        assertThat(presignedResponse.getNumFailed()).isZero();

        assertThat(presignedResponse.getDocuments()).hasSize(1);
        var doc = presignedResponse.getDocuments().get(0);
        assertThat(doc.getSourceIndex()).isZero();
        assertThat(doc.getSourceUri()).isEqualTo("https://arxiv.org/pdf/2408.09869");
        assertThat(doc.getFilename()).isEqualTo("2408.09869");
        assertThat(doc.getStatus()).isEqualTo(ConversionStatus.SUCCESS);
        assertThat(doc.getErrors()).isEmpty();
        assertThat(doc.getArtifacts()).hasSize(1);

        var artifact = doc.getArtifacts().get(0);
        assertThat(artifact.getArtifactType()).isEqualTo(ArtifactType.MARKDOWN);
        assertThat(artifact.getMimeType()).isEqualTo("text/markdown");
        assertThat(artifact.getUri()).isEqualTo(URI.create("https://storage.example.com/2408.09869.md"));
        assertThat(artifact.getUrlExpiresAt()).isNotNull();
    }

    @Test
    void shouldConvertMultipleSourcesWithPresignedUrlTarget() {
        wiremock().register(
                post(urlPathEqualTo("/v1/convert/source"))
                        .withHeader(HttpHeaders.CONTENT_TYPE, containing(MediaType.APPLICATION_JSON))
                        .willReturn(okJson("""
                                {
                                  "processing_time": 8.27,
                                  "num_converted": 2,
                                  "num_succeeded": 2,
                                  "num_partially_succeeded": 0,
                                  "num_failed": 0,
                                  "documents": [
                                    {
                                      "source_index": 0,
                                      "source_uri": "https://arxiv.org/pdf/2408.09869",
                                      "filename": "2408.09869",
                                      "status": "success",
                                      "artifacts": [
                                        {
                                          "artifact_type": "markdown",
                                          "mime_type": "text/markdown",
                                          "uri": "https://storage.example.com/2408.09869.md"
                                        }
                                      ]
                                    },
                                    {
                                      "source_index": 1,
                                      "source_uri": "https://arxiv.org/pdf/2501.17887",
                                      "filename": "2501.17887",
                                      "status": "success",
                                      "artifacts": [
                                        {
                                          "artifact_type": "markdown",
                                          "mime_type": "text/markdown",
                                          "uri": "https://storage.example.com/2501.17887.md"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """)));

        var request = ConvertDocumentRequest.builder()
                .source(HttpSource.builder()
                        .url(URI.create("https://arxiv.org/pdf/2408.09869"))
                        .build())
                .source(HttpSource.builder()
                        .url(URI.create("https://arxiv.org/pdf/2501.17887"))
                        .build())
                .target(PresignedUrlTarget.builder().build())
                .build();

        var response = doclingApi.convertSource(request);

        assertThat(response)
                .isNotNull()
                .isInstanceOf(PreSignedUrlConvertResponse.class);

        assertThat(response.getResponseType())
                .isEqualTo(ResponseType.PRE_SIGNED_URL_RESPONSE);

        var presignedResponse = (PreSignedUrlConvertResponse) response;
        assertThat(presignedResponse.getNumConverted()).isEqualTo(2);
        assertThat(presignedResponse.getNumSucceeded()).isEqualTo(2);
        assertThat(presignedResponse.getDocuments()).hasSize(2);
        assertThat(presignedResponse.getDocuments().get(0).getFilename()).isEqualTo("2408.09869");
        assertThat(presignedResponse.getDocuments().get(1).getFilename()).isEqualTo("2501.17887");
        assertThat(presignedResponse.getDocuments().get(1).getSourceIndex()).isEqualTo(1);
    }
}
