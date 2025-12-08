package io.quarkiverse.docling.deployment.devservices;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import io.quarkiverse.docling.deployment.config.DoclingBuildTimeConfig;
import io.quarkiverse.docling.deployment.devservices.config.DoclingDevServicesConfig;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.PodmanStatusBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig.Enabled;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = Enabled.class)
class DoclingDevServicesProcessor {
    private static final Logger LOG = Logger.getLogger(DoclingDevServicesProcessor.class);

    public static final String FEATURE = "docling-dev-service";
    public static final String PROVIDER = "docling";

    /**
     * Label to add to shared Dev Service for Docling running in containers.
     * This allows other applications to discover the running service and use it instead of starting a new instance.
     */
    static final String DEV_SERVICE_LABEL = "quarkus-dev-service-docling";

    private static volatile RunningDevService DEV_SERVICE;
    private static volatile DoclingDevServicesConfig DEV_SERVICES_CONFIG;
    private static volatile boolean FIRST = true;

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void startDoclingContainer(
            DoclingBuildTimeConfig doclingBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            PodmanStatusBuildItem podmanStatusBuildItem,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            CuratedApplicationShutdownBuildItem shutdownBuildItem,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            BuildProducer<DoclingDevServicesConfigBuildItem> doclingDevServicesConfigBuildProducer,
            BuildProducer<DevServicesResultBuildItem> devServicesResultProducer) {

        var doclingDevServicesBuildConfig = doclingBuildTimeConfig.devservices();

        // Figure out if we need to shut down and restart the existing container
        // If not, and the container has already started, just return
        if (DEV_SERVICE != null) {
            var restartRequired = !doclingDevServicesBuildConfig.equals(DEV_SERVICES_CONFIG);

            if (!restartRequired) {
                devServicesResultProducer.produce(DEV_SERVICE.toBuildItem());
            }

            shutdown();
            DEV_SERVICES_CONFIG = null;
        }

        if (isDoclingRunning()) {
            LOG.infof("Not starting Docling dev services container as it is already running on port %d",
                    DoclingServeContainer.DEFAULT_DOCLING_PORT);
            return;
        }

        // Re-initialize captured config and dev services
        DEV_SERVICES_CONFIG = doclingDevServicesBuildConfig;
        var logCompressor = new StartupLogCompressor(
                "%sDocling Dev Service Starting:".formatted(launchMode.isTest() ? "(test) " : ""),
                consoleInstalledBuildItem,
                loggingSetupBuildItem);

        try {
            startContainer(dockerStatusBuildItem, podmanStatusBuildItem, doclingDevServicesBuildConfig,
                    !devServicesSharedNetworkBuildItem.isEmpty())
                    .ifPresentOrElse(
                            devService -> {
                                DEV_SERVICE = devService;
                                logCompressor.close();
                            },
                            logCompressor::closeAndDumpCaptured);
        } catch (Throwable t) {
            logCompressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (DEV_SERVICE != null) {
            LOG.info("Dev services for Docling started");
            devServicesResultProducer.produce(DEV_SERVICE.toBuildItem());
            doclingDevServicesConfigBuildProducer.produce(new DoclingDevServicesConfigBuildItem(DEV_SERVICE.getConfig()));

            if (FIRST) {
                FIRST = false;

                // Add close tasks on first run only
                Runnable closeTask = () -> {
                    if (DEV_SERVICE != null) {
                        shutdown();
                        LOG.info("Dev Services for Docling has been shut down.");
                    }

                    FIRST = true;
                    DEV_SERVICE = null;
                    DEV_SERVICES_CONFIG = null;
                };

                shutdownBuildItem.addCloseTask(closeTask, true);
            }
        } else {
            var baseUrl = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                    .getConfigValue(DoclingRuntimeConfig.BASE_URL_KEY).getValue();

            if ((baseUrl != null) && !baseUrl.isEmpty()) {
                doclingDevServicesConfigBuildProducer
                        .produce(new DoclingDevServicesConfigBuildItem(Map.of(DoclingRuntimeConfig.BASE_URL_KEY, baseUrl)));
            }
        }
    }

    private Optional<RunningDevService> startContainer(
            DockerStatusBuildItem dockerStatusBuildItem,
            PodmanStatusBuildItem podmanStatusBuildItem,
            DoclingDevServicesConfig doclingDevServicesConfig,
            boolean useSharedNetwork) {

        if (!doclingDevServicesConfig.enabled()) {
            LOG.warn("Not starting dev services for Docling as it has been disabled in the config.");
            return Optional.empty();
        }

        if (ConfigUtils.isPropertyNonEmpty(DoclingRuntimeConfig.BASE_URL_KEY)) {
            LOG.warnf("Not starting dev services for Docling as the %s property is set.", DoclingRuntimeConfig.BASE_URL_KEY);
            return Optional.empty();
        }

        var podmanAvailable = podmanStatusBuildItem.isContainerRuntimeAvailable();
        var dockerAvailable = dockerStatusBuildItem.isContainerRuntimeAvailable();
        var isAContainerRuntimeAvailable = podmanAvailable || dockerAvailable;

        if (!isAContainerRuntimeAvailable) {
            LOG.warn("Not starting dev services for Docling as the container runtime is not available.");
            return Optional.empty();
        }

        var doclingContainer = new DoclingContainer(doclingDevServicesConfig, useSharedNetwork);
        doclingContainer.start();

        return Optional.of(
                new RunningDevService(
                        PROVIDER,
                        doclingContainer.getContainerId(),
                        doclingContainer::close,
                        doclingContainer.getExposedConfig()));
    }

    private void shutdown() {
        if (DEV_SERVICE != null) {
            try {
                LOG.info("Dev Services for Docling shutting down...");
                DEV_SERVICE.close();
            } catch (Throwable t) {
                LOG.error("Failed to shut down dev services for Docling", t);
            } finally {
                DEV_SERVICE = null;
            }
        }
    }

    private boolean isDoclingRunning() {
        try (var s = new Socket("localhost", DoclingServeContainer.DEFAULT_DOCLING_PORT)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
