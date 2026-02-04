package io.quarkiverse.docling.deployment.devui;

import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;
import io.quarkiverse.docling.deployment.devservices.DoclingContainer;
import io.quarkiverse.docling.deployment.devservices.DoclingDevServicesProcessor;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

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
    CardPageBuildItem devuiCard() {
        LOG.info("Inside DoclingDevUIProcessor.devuiCard");
        var card = new CardPageBuildItem();
        //        findDoclingDevService(devServices)
        //                .ifPresent(doclingDevService -> buildCard(doclingDevService, card));

        return card;
    }

    private static void buildCard(DevServicesResultBuildItem doclingDevService, CardPageBuildItem card) {
        LOG.infof("Description: %s", doclingDevService.getDescription());
        Optional.ofNullable(doclingDevService.getServiceConfig())
                .filter(DoclingServeContainerConfig.class::isInstance)
                .map(DoclingServeContainerConfig.class::cast)
                .ifPresent(config -> {
                    //                    LOG.infof("Config: %s", config.asString());
                    LOG.infof("Api doc URL: %s",
                            ConfigUtils.getFirstOptionalValue(List.of(DoclingContainer.CONFIG_DOCLING_API_DOC), String.class));

                    // The config here is empty?
                    // Maybe because the service hasn't fully started yet?

                    //                    configProvider.getOptionalValue(DoclingContainer.CONFIG_DOCLING_API_DOC, String.class)
                    //                            .ifPresent(apiDocUrl -> card.addPage(
                    //                                    Page.externalPageBuilder("Swagger UI")
                    //                                            .url(apiDocUrl)
                    //                                            .isHtmlContent()));

                    //                  Optional.ofNullable(config.get(DoclingContainer.CONFIG_DOCLING_UI))
                    //                      .ifPresent(uiUrl -> card.addPage(
                    //                          Page.externalPageBuilder("Docling UI")
                    //                              .url(uiUrl)
                    //                              .isHtmlContent()));
                    //
                    //                  Optional.ofNullable(config.get(DoclingContainer.CONFIG_DOCLING_API_SCALAR_DOC))
                    //                      .ifPresent(scalarUrl -> card.addPage(
                    //                          Page.externalPageBuilder("Scalar UI")
                    //                              .url(scalarUrl)
                    //                              .isHtmlContent()));
                });
    }

    private static Optional<DevServicesResultBuildItem> findDoclingDevService(
            List<DevServicesResultBuildItem> devServicesResults) {
        return devServicesResults.stream()
                .peek(devService -> LOG.infof("Dev service: [%s, %s, %s, %s]", devService.getName(),
                        devService.getDescription(), devService.getServiceName(), devService.getServiceConfig()))
                .filter(devService -> DoclingDevServicesProcessor.PROVIDER.equals(devService.getName()))
                .findFirst();
    }
}
