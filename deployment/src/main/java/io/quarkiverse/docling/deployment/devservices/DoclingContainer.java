package io.quarkiverse.docling.deployment.devservices;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jboss.logging.Logger;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import io.quarkiverse.docling.deployment.devservices.config.DoclingDevServicesConfig;
import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;
import io.quarkus.devservices.common.ConfigureUtil;

public class DoclingContainer extends DoclingServeContainer {
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

    private final boolean useSharedNetwork;

    /**
     * The dynamic host name determined from TestContainers
     */
    private String hostName;
    private final DoclingDevServicesConfig config;

    DoclingContainer(DoclingDevServicesConfig config, boolean useSharedNetwork) {
        super(config);
        this.config = config;
        this.useSharedNetwork = useSharedNetwork;
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

    @Override
    public String getHost() {
        return ((this.hostName != null) && !this.hostName.isEmpty()) ? this.hostName : super.getHost();
    }
}
