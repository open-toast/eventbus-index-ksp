plugins {
    `kotlin-conventions`
    `publishing-conventions`
    alias(libs.plugins.ksp1)
}

dependencies {
    ksp(libs.autoservice.ksp)
    implementation(libs.ksp.api)
    implementation(libs.autoservice.annotations)
    implementation(libs.kotlinPoet)
    implementation(libs.kotlinPoetKsp)
}