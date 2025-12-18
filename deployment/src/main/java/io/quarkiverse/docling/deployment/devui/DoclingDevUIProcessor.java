package io.quarkiverse.docling.deployment.devui;

import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.dev.devservices.DevServiceDescriptionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

import io.quarkiverse.docling.deployment.devservices.DoclingContainer;
import io.quarkiverse.docling.deployment.devservices.DoclingDevServicesProcessor;

class DoclingDevUIProcessor {
    private static final Logger LOG = Logger.getLogger(DoclingDevUIProcessor.class);

    //    @BuildStep(onlyIf = IsLocalDevelopment.class)
    //    CardPageBuildItem doIt(DevServicesRegistryBuildItem devServicesRegistry, DoclingBuildTimeConfig doclingBuildTimeConfig) {
    //        var runningServices = devServicesRegistry.getRunningServices(
    //                DoclingDevServicesProcessor.PROVIDER,
    //                doclingBuildTimeConfig.devservices().serviceName(),
    //                doclingBuildTimeConfig.devservices());
    //
    //        LOG.infof("Running services: %s", runningServices);
    //        LOG.infof("Config: %s", devServicesRegistry.getConfigForAllRunningServices());
    //        return new CardPageBuildItem();
    //    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    CardPageBuildItem devuiCard(List<DevServiceDescriptionBuildItem> devServices) {
        var card = new CardPageBuildItem();
        findDoclingDevService(devServices)
                .ifPresent(doclingDevService -> buildCard(doclingDevService, card));

        return card;
    }

    private static void buildCard(DevServiceDescriptionBuildItem doclingDevService, CardPageBuildItem card) {
        LOG.infof("Container info: %s", doclingDevService.getContainerInfo());
        LOG.infof("Description: %s", doclingDevService.getDescription());
        var config = doclingDevService.getConfigs();
        LOG.infof("Config: %s", config);

        // The config here is empty?
        // Maybe because the service hasn't fully started yet?

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
    }

    private static Optional<DevServiceDescriptionBuildItem> findDoclingDevService(
            List<DevServiceDescriptionBuildItem> devServicesResults) {
        return devServicesResults.stream()
                .peek(devService -> LOG.infof("Dev service: [%s, %s, %s, %s]", devService.getName(),
                        devService.getDescription(), devService.getContainerInfo(), devService.getConfigs()))
                .filter(devService -> DoclingDevServicesProcessor.PROVIDER.equals(devService.getName()))
                .findFirst();
    }
}
