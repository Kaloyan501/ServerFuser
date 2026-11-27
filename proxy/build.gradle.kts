@file:Suppress("SpellCheckingInspection")

plugins {
    id("java-library")
    id("xyz.jpenilla.run-velocity") version "3.1.0"
}



repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.2.1-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.2.1-SNAPSHOT")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    implementation(project(":protocol"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.jar {
    dependsOn(configurations.runtimeClasspath)

    from({
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/*.EC",
        "META-INF/SIG-*"
    )
}

tasks {
    runVelocity {
        velocityVersion("3.4.0-SNAPSHOT")
    }
}