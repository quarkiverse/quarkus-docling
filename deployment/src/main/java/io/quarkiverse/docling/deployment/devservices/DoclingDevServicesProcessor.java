package io.quarkiverse.docling.deployment.devservices;

import java.net.Socket;
import java.util.List;

import org.jboss.logging.Logger;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import io.quarkiverse.docling.deployment.config.DoclingBuildTimeConfig;
import io.quarkiverse.docling.deployment.devservices.config.DoclingDevServicesConfig;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.deployment.IsProduction;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.dev.devservices.DevServicesConfig.Enabled;
import io.quarkus.devservices.common.ComposeLocator;
import io.quarkus.devservices.common.ContainerLocator;
import io.quarkus.runtime.LaunchMode;

@BuildSteps(onlyIfNot = IsProduction.class, onlyIf = Enabled.class)
public class DoclingDevServicesProcessor {
    private static final Logger LOG = Logger.getLogger(DoclingDevServicesProcessor.class);

    public static final String FEATURE = "docling-dev-service";
    public static final String PROVIDER = "docling";

    /**
     * Label to add to shared Dev Service for Docling running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-docling";

    private static final ContainerLocator DOCLING_CONTAINER_LOCATOR = ContainerLocator
            .locateContainerWithLabels(DoclingServeContainer.DEFAULT_DOCLING_PORT, DEV_SERVICE_LABEL);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void startDoclingDevServices(
            LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DoclingBuildTimeConfig doclingBuildTimeConfig,
            BuildProducer<DevServicesResultBuildItem> devServicesResult,
            DevServicesConfig devServicesConfig) {

        var doclingDevServicesConfig = doclingBuildTimeConfig.devservices();

        if (!doclingDevServicesDisabled(dockerStatusBuildItem, doclingDevServicesConfig)) {
            var useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                    devServicesSharedNetworkBuildItem);

            var devServicesResultBuildItem = discoverRunningService(composeProjectBuildItem, doclingDevServicesConfig,
                    launchMode.getLaunchMode(), useSharedNetwork);

            devServicesResult.produce(devServicesResultBuildItem);
            LOG.info("produced DevServicesResultBuildItem");
        }
    }

    private static DevServicesResultBuildItem discoverRunningService(DevServicesComposeProjectBuildItem composeProjectBuildItem,
            DoclingDevServicesConfig devServicesConfig,
            LaunchMode launchMode,
            boolean useSharedNetwork) {

        return DOCLING_CONTAINER_LOCATOR
                .locateContainer(devServicesConfig.serviceName(), devServicesConfig.shared(), launchMode)
                .or(() -> ComposeLocator.locateContainer(composeProjectBuildItem,
                        List.of(devServicesConfig.image()), DoclingServeContainer.DEFAULT_DOCLING_PORT, launchMode,
                        useSharedNetwork))
                .map(containerAddress -> DevServicesResultBuildItem.discovered()
                        .name(PROVIDER)
                        .containerId(containerAddress.getId())
                        .description("Docling Serve")
                        .config(DoclingContainer.getExposedConfig(devServicesConfig, containerAddress.getHost(),
                                containerAddress.getPort()))
                        .build())
                .orElseGet(() -> DevServicesResultBuildItem.owned()
                        .name(PROVIDER)
                        .description("Docling Serve")
                        .serviceName(devServicesConfig.serviceName())
                        .serviceConfig(devServicesConfig)
                        .startable(() -> new DoclingContainer(devServicesConfig, useSharedNetwork))
                        .postStartHook(s -> logDevServiceStarted(s.getConnectionInfo()))
                        .configProvider(DoclingContainer.getExposedConfig(devServicesConfig))
                        .build());
    }

    private static void logDevServiceStarted(String connectionInfo) {
        LOG.infof("Dev Services for Docling started. Other applications in dev mode will find the " +
                "it automatically. For Quarkus application in production mode, you can connect to " +
                "this coordinator by starting you application with -D%s=%s\n", DoclingRuntimeConfig.BASE_URL_KEY,
                connectionInfo);
    }

    private static boolean doclingDevServicesDisabled(DockerStatusBuildItem dockerStatusBuildItem,
            DoclingDevServicesConfig devServicesConfig) {
        if (isDoclingRunning()) {
            LOG.infof("Not starting Docling dev services container as it is already running on port %d",
                    DoclingServeContainer.DEFAULT_DOCLING_PORT);
            return true;
        }

        if (!devServicesConfig.enabled()) {
            // explicitly disabled
            LOG.info("Not starting devservices for docling as it has been disabled in the config");
            return true;
        }

        if (!dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            LOG.warn("Please get a working container runtime");
            return true;
        }

        return false;
    }

    private static boolean isDoclingRunning() {
        try (var s = new Socket("localhost", DoclingServeContainer.DEFAULT_DOCLING_PORT)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
