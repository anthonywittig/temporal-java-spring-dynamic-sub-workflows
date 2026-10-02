dependencies {
    implementation(project(":api"))
    implementation(libs.temporal.sdk)
    // Activity implementations are Spring beans discovered by the worker.
    compileOnly(libs.temporal.spring.boot.autoconfigure)
    compileOnly("org.springframework:spring-context")
    implementation("org.slf4j:slf4j-api")

    testImplementation(project(":workflow"))
    testImplementation(project(":testkit"))
    testImplementation(libs.temporal.testing)
    testImplementation("org.awaitility:awaitility")
}
