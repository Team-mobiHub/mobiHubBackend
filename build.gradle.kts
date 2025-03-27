plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.mobihub"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

sourceSets {
    test {
        resources.srcDirs += file("$projectDir/src/main/resources")
    }
}

dependencies {
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation(libs.logback.classic)
    implementation(libs.jakarta.mail)
    implementation(libs.ktor.server.config.yaml)
    implementation("org.mindrot:jbcrypt:0.4")

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("org.mockito:mockito-inline:5.0.0")
    testImplementation("org.mindrot:jbcrypt:0.4")
    testImplementation("io.mockk:mockk:1.9.3")
}
