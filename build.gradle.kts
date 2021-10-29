import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
//    id("io.spring.dependency-management") version "1.0.11.RELEASE"
//    id("org.springframework.boot") version "2.4.11"
}

group = "me.joostdevries"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
val coroutinesVersion = "1.5.2"
dependencies {
    //implementation(platform("org.springframework.boot:spring-boot-dependencies:2.4.11"))
    implementation("org.springframework:spring-webflux:5.3.10")
    implementation("io.projectreactor.netty:reactor-netty-http:1.0.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.tomakehurst:wiremock-jre8:2.31.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions{
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=kotlin.RequiresOptIn,kotlin.time.ExperimentalTime",
            "-Xskip-metadata-version-check",
            "-Xjsr305=strict"
        )
    }
}
