plugins {
    `kotlin-conventions`
    alias(libs.plugins.ksp)
}

ksp {
    arg("eventBusIndex", "com.toasttab.eventbus.ksp.test.TestIndex")
}

dependencies {
    ksp(project(":eventbus-index-ksp-processor"))
    implementation(libs.eventbus)
}