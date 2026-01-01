package io.quarkiverse.docling.deployment.devservices;

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.deployment.builditem.Startable;
import io.quarkus.devservices.common.ConfigureUtil;

import io.quarkiverse.docling.runtime.config.DoclingRuntimeConfig;

import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

public class DoclingContainer extends DoclingServeContainer implements Startable {
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
     * Configuration key for the Scalar documentation endpoint used by the Docling dev service
     */
    public static final String CONFIG_DOCLING_API_SCALAR_DOC = DoclingDevServicesProcessor.FEATURE + ".docling.api.scalar.doc";

    /**
     * Configuration key for the UI endpoint used by the Docling dev service
     */
    public static final String CONFIG_DOCLING_UI = DoclingDevServicesProcessor.FEATURE + ".docling.ui";

    private final boolean useSharedNetwork;

    /**
     * The dynamic host name determined from TestContainers
     */
    private String hostName;
    private final DoclingServeContainerConfig config;

    DoclingContainer(DoclingServeContainerConfig config, boolean useSharedNetwork) {
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

    static Map<String, Function<DoclingContainer, String>> getExposedConfig(DoclingServeContainerConfig config) {
        var exposed = new HashMap<String, Function<DoclingContainer, String>>(7);
        Function<DoclingContainer, String> apiEndpointFunction = DoclingContainer::getApiUrl;

        exposed.put(CONFIG_DOCLING_PORT, c -> Objects.toString(c.getPort()));
        exposed.put(CONFIG_DOCLING_HTTP_SERVER, c -> c.getHost());
        exposed.put(CONFIG_DOCLING_API_ENDPOINT, apiEndpointFunction);
        exposed.put(CONFIG_DOCLING_API_DOC, c -> "%s/docs".formatted(apiEndpointFunction.apply(c)));
        exposed.put(CONFIG_DOCLING_API_SCALAR_DOC, c -> "%s/scalar".formatted(apiEndpointFunction.apply(c)));
        exposed.put(DoclingRuntimeConfig.BASE_URL_KEY, apiEndpointFunction);

        Optional.ofNullable(config.apiKey())
                .ifPresent(apiKey -> exposed.put(DoclingRuntimeConfig.API_KEY_KEY, c -> apiKey));

        if (config.enableUi()) {
            exposed.put(CONFIG_DOCLING_UI, c -> "%s/ui".formatted(apiEndpointFunction.apply(c)));
        }

        return exposed.entrySet()
                .stream()
                .collect(toMap(Entry::getKey, entry -> entry.getValue().andThen(s -> {
                    LOG.infof("%s=%s", entry.getKey(), s);
//                    throw new RuntimeException();
                  return s;
                })));

        //        return exposed;
    }

    public static Map<String, String> getExposedConfig(DoclingServeContainerConfig config, String apiUrl) {
        var parts = apiUrl.split(":");
        return getExposedConfig(config, parts[0], Integer.parseInt(parts[1]));
    }

    static Map<String, String> getExposedConfig(DoclingServeContainerConfig config, String host, int port) {
        var apiEndpoint = "http://%s:%d".formatted(host, port);
        var exposed = new HashMap<String, String>(7);

        exposed.put(CONFIG_DOCLING_PORT, Objects.toString(port));
        exposed.put(CONFIG_DOCLING_HTTP_SERVER, host);
        exposed.put(CONFIG_DOCLING_API_ENDPOINT, apiEndpoint);
        exposed.put(CONFIG_DOCLING_API_DOC, "%s/docs".formatted(apiEndpoint));
        exposed.put(CONFIG_DOCLING_API_SCALAR_DOC, "%s/scalar".formatted(apiEndpoint));
        exposed.put(DoclingRuntimeConfig.BASE_URL_KEY, apiEndpoint);

        Optional.ofNullable(config.apiKey())
                .ifPresent(apiKey -> exposed.put(DoclingRuntimeConfig.API_KEY_KEY, apiKey));

        if (config.enableUi()) {
            exposed.put(CONFIG_DOCLING_UI, "%s/ui".formatted(apiEndpoint));
        }

        return exposed;
    }

    /**
     * Info about the DevService.
     *
     * @return the map of as running configuration of the dev service
     */
    public Map<String, String> getExposedConfig() {
        var exposed = new HashMap<>(getExposedConfig(this.config).entrySet()
                .stream()
                .collect(toMap(Entry::getKey, e -> e.getValue().apply(this))));

        exposed.putAll(getEnvMap());

        return exposed;
    }

    @Override
    public String getHost() {
        return ((this.hostName != null) && !this.hostName.isEmpty()) ? this.hostName : super.getHost();
    }

    @Override
    public String getConnectionInfo() {
        return getApiUrl();
    }

    @Override
    public void close() {
        super.close();
    }
}
