
plugins {
    application
    kotlin("jvm") version "1.9.23"
}



repositories {
    mavenCentral()
}

application{
    mainClass = "org.monzo.crawler"
}

tasks.withType<Jar> {
    // Otherwise you'll get a "No main manifest attribute" error
    manifest {
        attributes["Main-Class"] = "org.monzo.crawler.Main"
    }

    // To avoid the duplicate handling strategy error
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // To add all of the dependencies
    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}


dependencies {
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.10")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}