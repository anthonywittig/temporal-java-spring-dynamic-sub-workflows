plugins {
    id("org.springframework.boot")
}

// Note: no dependency on :workflow or :handlers. The consumer starts workflows by type name.
dependencies {
    implementation(project(":api"))
    implementation(libs.temporal.spring.boot.starter)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.kafka:spring-kafka")

    testImplementation(project(":testkit"))
    testImplementation(libs.temporal.testing)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility")
}
