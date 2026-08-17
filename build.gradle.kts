plugins {
    kotlin("jvm") version "2.4.10"
    application
}

group = "com.sparta"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.5.2")
    implementation("io.ktor:ktor-server-netty:3.5.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.5.2")
    implementation("io.ktor:ktor-serialization-jackson:3.5.2")
    implementation("io.ktor:ktor-server-status-pages:3.5.2")

    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.13")

    implementation("org.slf4j:slf4j-api:2.0.18")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.26.1")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.26.1")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:3.5.2")
    testImplementation("io.ktor:ktor-client-content-negotiation:3.5.2")
    testImplementation("io.kotest:kotest-assertions-core:6.2.3")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.sparta.interviewplatform.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
