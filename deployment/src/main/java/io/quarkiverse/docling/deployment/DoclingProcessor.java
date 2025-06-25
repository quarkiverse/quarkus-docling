package io.quarkiverse.docling.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkiverse.docling.runtime.DoclingRecorder;
import io.quarkiverse.docling.runtime.client.DoclingService;
import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class DoclingProcessor {
    private static final String FEATURE = "docling";
    private static final DotName DOCLING_CLIENT = DotName.createSimple(DoclingApi.class);
    private static final DotName DOCLING_SERVICE = DotName.createSimple(DoclingService.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void generateDoclingBeans(DoclingRecorder recorder, BuildProducer<SyntheticBeanBuildItem> beanProducer) {
        beanProducer.produce(
                SyntheticBeanBuildItem
                        .configure(DOCLING_CLIENT)
                        .setRuntimeInit()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .supplier(recorder.doclingClient())
                        .done());

        beanProducer.produce(
                SyntheticBeanBuildItem
                        .configure(DOCLING_SERVICE)
                        .setRuntimeInit()
                        .defaultBean()
                        .scope(ApplicationScoped.class)
                        .createWith(recorder.doclingService())
                        .addInjectionPoint(ClassType.create(DOCLING_CLIENT))
                        .done());
    }
}
