import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    `java-gradle-plugin`
    idea
//    id("io.spring.dependency-management") version "1.0.11.RELEASE"
//    id("org.springframework.boot") version "2.4.11"
}

group = "me.joostdevries"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
val coroutinesVersion = "1.6.0"
dependencies {
    //implementation(platform("org.springframework.boot:spring-boot-dependencies:2.4.11"))
    implementation("org.springframework:spring-webflux:5.3.10")
    implementation("io.projectreactor.netty:reactor-netty-http:1.0.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")
    implementation("io.gatling.highcharts:gatling-charts-highcharts:3.3.1")

    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.tomakehurst:wiremock-jre8:2.31.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions{
        languageVersion = "1.6"
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.OptIn",
            "-Xskip-metadata-version-check",
            "-Xjsr305=strict"
        )
    }
}
