package io.quarkiverse.docling;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;

import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.convert.response.InBodyConvertDocumentResponse;

public class AssertionHelper {
    public static void assertStoryPdfResponse(ConvertDocumentResponse response) {
        assertStoryPdfResponse(response, "story.pdf");
    }

    public static void assertStoryPdfResponse(ConvertDocumentResponse response, String filename) {
        assertThat(response)
                .isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.type(InBodyConvertDocumentResponse.class))
                .satisfies(r -> {
                    assertThat(r.getStatus()).as("response status")
                            .isNotEmpty();

                    assertThat(r.getDocument())
                            .as("response document")
                            .isNotNull()
                            .satisfies(doc -> {
                                assertThat(doc.getFilename()).isEqualTo(filename);
                                assertThat(doc.getMarkdownContent()).isNotEmpty();
                            });

                    if (r.getProcessingTime() != null) {
                        assertThat(r.getProcessingTime()).isPositive();
                    }
                });
    }
}
