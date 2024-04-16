plugins {
    application
    kotlin("jvm") version "1.9.23"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.10")
}

application{
    mainClass = "org.monzo.crawler"
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}