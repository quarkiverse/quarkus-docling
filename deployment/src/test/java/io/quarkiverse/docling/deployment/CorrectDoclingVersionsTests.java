package io.quarkiverse.docling.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

import io.quarkiverse.docling.deployment.config.DoclingBuildTimeConfig;
import io.quarkiverse.docling.deployment.devservices.config.DoclingDevServicesConfig;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkiverse.docling.testing.internal.WiremockAware;
import io.swagger.v3.parser.OpenAPIV3Parser;

class CorrectDoclingVersionsTests extends WiremockAware {
    private static final Path OPENAPI_JSON_PATH = Path.of("..", "runtime", "src", "main", "openapi", "docling.json");

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideConfigKey("quarkus.docling.devservices.enabled", "false")
            .overrideRuntimeConfigKey(DoclingRuntimeConfig.BASE_URL_KEY, wiremockUrlForConfig());

    @Test
    void doclingContainerVersionMatchesClientVersion() throws IOException {
        var openApiVersion = "v%s".formatted(getOpenAPIVersion());
        var doclingContainerVersion = getDevServiceContainerImageVersion();

        assertThat(openApiVersion)
                .withFailMessage(
                        () -> """

                                OpenAPI version '%s' does not match dev service container image version '%s'.

                                You need to either update the OpenAPI document (runtime/src/main/openapi/docling.json) or update the dev service container image version (deployment/src/main/java/%s.java#DOCLING_IMAGE).

                                You can find the container image versions at https://quay.io/repository/docling-project/docling-serve?tab=tags
                                """
                                .formatted(openApiVersion, doclingContainerVersion,
                                        DoclingBuildTimeConfig.class.getName().replace('.', '/')))
                .isEqualTo(doclingContainerVersion);
    }

    private String getDevServiceContainerImageVersion() {
        var doclingImageName = DoclingDevServicesConfig.DOCLING_IMAGE;
        var imageParts = doclingImageName.split(":");

        assertThat(imageParts)
                .isNotNull()
                .hasSize(2);

        return imageParts[1];
    }

    private String getOpenAPIVersion() throws IOException {
        var doclingJsonContents = Files.readString(OPENAPI_JSON_PATH);
        var openApiParseResult = new OpenAPIV3Parser().readContents(doclingJsonContents, null, null);

        return openApiParseResult.getOpenAPI().getInfo().getVersion();
    }
}
