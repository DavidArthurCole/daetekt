plugins {
    id("module")
}

dependencies {
    api(projects.detektApi)
    api(libs.kotlin.analysisApiSurface)
    implementation(libs.kotlin.analysisApiImplementation)
    api(libs.kotlin.analysisApiIntellijApiSurfaceComponents)
    testImplementation(projects.detektTestUtils)
    testImplementation(libs.assertj.core)
}
