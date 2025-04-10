plugins {
    `kotlin-conventions`
    `publishing-conventions`
    alias(libs.plugins.ksp1)
}

dependencies {
    ksp(libs.autoservice.ksp)
    compileOnly(libs.ksp.api)
    compileOnly(libs.autoservice.annotations)
    implementation(libs.kotlinPoet)
    implementation(libs.kotlinPoetKsp)
}