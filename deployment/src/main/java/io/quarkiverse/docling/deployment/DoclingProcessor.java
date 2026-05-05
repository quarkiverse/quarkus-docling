package io.quarkiverse.docling.deployment;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.spi.DoclingServeApiBuilderFactory;
import io.quarkiverse.docling.runtime.DoclingRecorder;
import io.quarkiverse.docling.runtime.client.DoclingService;
import io.quarkiverse.docling.runtime.client.QuarkusDoclingServeApiBuilderFactory;
import io.quarkiverse.docling.runtime.client.QuarkusDoclingServeClient;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

class DoclingProcessor {
    private static final String FEATURE = "docling";
    private static final DotName QUARKUS_DOCLING_SERVE_CLIENT = DotName.createSimple(QuarkusDoclingServeClient.class);
    private static final DotName DOCLING_SERVE_API = DotName.createSimple(DoclingServeApi.class);
    private static final DotName DOCLING_SERVICE = DotName.createSimple(DoclingService.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServiceProviderBuildItem nativeImageServiceProviderRegistration() {
        return new ServiceProviderBuildItem(DoclingServeApiBuilderFactory.class.getName(),
                QuarkusDoclingServeApiBuilderFactory.class.getName());
    }

    @BuildStep
    void indexDoclingClasses(BuildProducer<IndexDependencyBuildItem> indexDependency) {
        indexDependency.produce(List.of(
                new IndexDependencyBuildItem("ai.docling", "docling-serve-api"),
                new IndexDependencyBuildItem("ai.docling", "docling-core")));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void generateDoclingBeans(DoclingRecorder recorder, BuildProducer<SyntheticBeanBuildItem> beanProducer) {
        beanProducer.produce(
                SyntheticBeanBuildItem
                        .configure(QUARKUS_DOCLING_SERVE_CLIENT)
                        .setRuntimeInit()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .supplier(recorder.doclingServeClient())
                        .done());

        beanProducer.produce(
                SyntheticBeanBuildItem
                        .configure(DOCLING_SERVE_API)
                        .setRuntimeInit()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .createWith(recorder.doclingServeApi())
                        .addInjectionPoint(ClassType.create(QUARKUS_DOCLING_SERVE_CLIENT))
                        .done());

        beanProducer.produce(
                SyntheticBeanBuildItem
                        .configure(DOCLING_SERVICE)
                        .setRuntimeInit()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .createWith(recorder.doclingService())
                        .addInjectionPoint(ClassType.create(DOCLING_SERVE_API))
                        .done());
    }
}
