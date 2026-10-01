plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":workflow"))
    runtimeOnly(project(":handlers"))
    implementation(libs.temporal.spring.boot.starter)
    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation(project(":handlers"))
    testImplementation(project(":testkit"))
    testImplementation(libs.temporal.testing)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
