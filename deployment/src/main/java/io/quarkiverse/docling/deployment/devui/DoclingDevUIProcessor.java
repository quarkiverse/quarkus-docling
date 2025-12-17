package io.quarkiverse.docling.deployment.devui;

import java.util.List;
import java.util.Optional;

import io.quarkiverse.docling.deployment.devservices.DoclingContainer;
import io.quarkiverse.docling.deployment.devservices.DoclingDevServicesConfigBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

class DoclingDevUIProcessor {
    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem devuiCard(List<DoclingDevServicesConfigBuildItem> doclingDevServicesConfigs) {
        var card = new CardPageBuildItem();
        var config = ((doclingDevServicesConfigs != null) && (doclingDevServicesConfigs.size() == 1))
                ? doclingDevServicesConfigs.get(0).getConfig()
                : null;

        if (config != null) {
            Optional.ofNullable(config.get(DoclingContainer.CONFIG_DOCLING_API_DOC))
                    .ifPresent(apiDocUrl -> card.addPage(
                            Page.externalPageBuilder("Swagger UI")
                                    .url(apiDocUrl)
                                    .isHtmlContent()));

            Optional.ofNullable(config.get(DoclingContainer.CONFIG_DOCLING_UI))
                    .ifPresent(uiUrl -> card.addPage(
                            Page.externalPageBuilder("Docling UI")
                                    .url(uiUrl)
                                    .isHtmlContent()));

            Optional.ofNullable(config.get(DoclingContainer.CONFIG_DOCLING_API_SCALAR_DOC))
                    .ifPresent(scalarUrl -> card.addPage(
                            Page.externalPageBuilder("Scalar UI")
                                    .url(scalarUrl)
                                    .isHtmlContent()));
        }

        return card;
    }
}
