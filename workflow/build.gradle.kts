dependencies {
    api(project(":api"))
    api(libs.temporal.sdk)
    // Only for the @WorkflowImpl annotation used by worker auto-discovery.
    compileOnly(libs.temporal.spring.boot.autoconfigure)
}

dependencies {
    testImplementation(libs.temporal.testing)
}
