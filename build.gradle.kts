plugins {
    alias(libs.plugins.spring.boot) apply false
}

subprojects {
    apply(plugin = "java-library")

    group = "com.example.poc"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    val libs = rootProject.libs
    dependencies {
        "implementation"(platform(libs.spring.boot.bom))
        "implementation"(platform(libs.temporal.bom))
        "testImplementation"(platform(libs.spring.boot.bom))
        "testImplementation"(platform(libs.temporal.bom))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.assertj:assertj-core")
        "testImplementation"("org.mockito:mockito-core")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        "testRuntimeOnly"("ch.qos.logback:logback-classic")
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
