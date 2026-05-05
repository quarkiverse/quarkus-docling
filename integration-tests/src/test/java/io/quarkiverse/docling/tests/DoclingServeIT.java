package io.quarkiverse.docling.tests;

import static io.restassured.RestAssured.get;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class DoclingServeIT {
    @ParameterizedTest
    @ValueSource(strings = { "health", "convertBytes", "convertFile", "convertFromUri", "convertFromBase64", "convertNullFile",
            "convertNonExistingFile", "convertNonFile" })
    void tests(String path) {
        get("/docling/%s".formatted(path)).then()
                .statusCode(204);
    }
}
