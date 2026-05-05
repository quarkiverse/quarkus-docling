package io.quarkiverse.docling.deployment.devui;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkiverse.docling.deployment.devservices.DoclingContainer;
import io.quarkiverse.docling.deployment.devservices.DoclingDevServicesProcessor;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

class DoclingDevUIProcessor {
    private static final Logger LOG = Logger.getLogger(DoclingDevUIProcessor.class);

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem devuiCard() {
        LOG.debug("Inside DoclingDevUIProcessor.devuiCard");
        var card = new CardPageBuildItem();
        card.addLibraryVersion("ai.docling", "docling-serve-api", "Docling Java",
                "https://docling-project.github.io/docling-java");

        card.addPage(
                Page.externalPageBuilder("Swagger UI")
                        .dynamicUrlJsonRPCMethodName(
                                "devui-dev-services:devServicesConfig",
                                Map.of(
                                        "name", DoclingDevServicesProcessor.FEATURE,
                                        "configKey", DoclingContainer.CONFIG_DOCLING_API_DOC))
                        .isHtmlContent());

        card.addPage(
                Page.externalPageBuilder("Docling UI")
                        .dynamicUrlJsonRPCMethodName(
                                "devui-dev-services:devServicesConfig",
                                Map.of(
                                        "name", DoclingDevServicesProcessor.FEATURE,
                                        "configKey", DoclingContainer.CONFIG_DOCLING_UI))
                        .isHtmlContent());

        card.addPage(
                Page.externalPageBuilder("Scalar UI")
                        .dynamicUrlJsonRPCMethodName(
                                "devui-dev-services:devServicesConfig",
                                Map.of(
                                        "name", DoclingDevServicesProcessor.FEATURE,
                                        "configKey", DoclingContainer.CONFIG_DOCLING_API_SCALAR_DOC))
                        .isHtmlContent());

        return card;
    }
}
