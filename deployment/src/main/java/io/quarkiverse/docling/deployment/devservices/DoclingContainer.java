package io.quarkiverse.docling.deployment.devservices;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkiverse.docling.deployment.devservices.config.DoclingDevServicesConfig;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.devservices.common.ConfigureUtil;

public class DoclingContainer extends GenericContainer<DoclingContainer> {
    private static final Logger LOG = Logger.getLogger(DoclingContainer.class);

    /**
     * Configuration key for the port number used by the Docling dev service
     */
    public static final String CONFIG_DOCLING_PORT = DoclingDevServicesProcessor.FEATURE + ".docling.port";

    /**
     * Configuration key for the host name used by the Docling dev service
     */
    public static final String CONFIG_DOCLING_HTTP_SERVER = DoclingDevServicesProcessor.FEATURE + ".docling.host";

    /**
     * Configuration key for the API endpoint used by the Docling dev service
     */
    public static final String CONFIG_DOCLING_API_ENDPOINT = DoclingDevServicesProcessor.FEATURE
            + ".docling.api.endpoint";

    /**
     * Configuration key for the documentation endpoint used by the Docling dev service
     */
    public static final String CONFIG_DOCLING_API_DOC = DoclingDevServicesProcessor.FEATURE + ".docling.api.doc";

    /**
     * Configuration key for the UI endpoint used by the Docling dev service
     */
    public static final String CONFIG_DOCLING_UI = DoclingDevServicesProcessor.FEATURE + ".docling.ui";

    /**
     * The default container port that docling runs on
     */
    public static final int DEFAULT_DOCLING_PORT = 5001;

    private final boolean useSharedNetwork;

    /**
     * The dynamic host name determined from TestContainers
     */
    private String hostName;
    private DoclingDevServicesConfig config;

    DoclingContainer(DoclingDevServicesConfig config, Optional<Duration> timeout, boolean useSharedNetwork) {
        super(DockerImageName.parse(config.imageName()).asCompatibleSubstituteFor(DoclingDevServicesConfig.DOCLING_IMAGE));
        this.config = config;
        this.useSharedNetwork = useSharedNetwork;

        // Configure the container
        withLabel(DoclingDevServicesProcessor.DEV_SERVICE_LABEL, DoclingDevServicesProcessor.PROVIDER);
        withExposedPorts(DEFAULT_DOCLING_PORT);
        withEnv(config.containerEnv());
        waitingFor(Wait.forHttp("/health"));

        if (config.enableUi()) {
            withEnv("DOCLING_SERVE_ENABLE_UI", "true");
        }

        timeout.ifPresentOrElse(super::withStartupTimeout, () -> withStartupTimeout(Duration.ofMinutes(1)));
    }

    @Override
    protected void configure() {
        super.configure();

        if (this.useSharedNetwork) {
            this.hostName = ConfigureUtil.configureSharedNetwork(this, DoclingDevServicesProcessor.PROVIDER);
        }
    }

    /**
     * Info about the DevService.
     *
     * @return the map of as running configuration of the dev service
     */
    public Map<String, String> getExposedConfig() {
        var host = getHost();
        var port = getPort();
        var apiEndpoint = "http://%s:%d".formatted(host, port);
        var exposed = new HashMap<String, String>(6);

        exposed.put(CONFIG_DOCLING_PORT, Objects.toString(port));
        exposed.put(CONFIG_DOCLING_HTTP_SERVER, host);
        exposed.put(CONFIG_DOCLING_API_ENDPOINT, apiEndpoint);
        exposed.put(CONFIG_DOCLING_API_DOC, "%s/docs".formatted(apiEndpoint));
        exposed.put(DoclingRuntimeConfig.BASE_URL_KEY, apiEndpoint);

        if (this.config.enableUi()) {
            exposed.put(CONFIG_DOCLING_UI, "%s/ui".formatted(apiEndpoint));
        }

        exposed.putAll(getEnvMap());

        return exposed;
    }

    public int getPort() {
        return getMappedPort(DEFAULT_DOCLING_PORT);
    }

    @Override
    public String getHost() {
        return ((this.hostName != null) && !this.hostName.isEmpty()) ? this.hostName : super.getHost();
    }
}
