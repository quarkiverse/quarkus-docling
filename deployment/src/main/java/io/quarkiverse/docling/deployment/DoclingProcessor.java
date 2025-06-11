package io.quarkiverse.docling.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class DoclingProcessor {
    private static final String FEATURE = "docling";
    //    private static final DotName DOCLING_CLIENT = DotName.createSimple(DoclingRestApi.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    //    @BuildStep
    //    @Record(ExecutionTime.RUNTIME_INIT)
    //    SyntheticBeanBuildItem generateDoclingRestClient(DoclingRecorder recorder) {
    //        return SyntheticBeanBuildItem
    //                .configure(DOCLING_CLIENT)
    //                .setRuntimeInit()
    //                .defaultBean()
    //                .scope(ApplicationScoped.class)
    //                .supplier(recorder.doclingClient())
    //                .done();
    //    }
}
