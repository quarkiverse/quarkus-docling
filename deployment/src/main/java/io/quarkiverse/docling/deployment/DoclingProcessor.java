package io.quarkiverse.docling.deployment;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.DotName;

import io.quarkiverse.docling.runtime.DoclingRecorder;
import io.quarkiverse.docling.runtime.client.api.DoclingApi;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class DoclingProcessor {
    private static final String FEATURE = "docling";
    private static final DotName DOCLING_CLIENT = DotName.createSimple(DoclingApi.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SyntheticBeanBuildItem generateDoclingRestClient(DoclingRecorder recorder) {
        return SyntheticBeanBuildItem
                .configure(DOCLING_CLIENT)
                .setRuntimeInit()
                .defaultBean()
                .scope(ApplicationScoped.class)
                .supplier(recorder.doclingClient())
                .done();
    }
}
