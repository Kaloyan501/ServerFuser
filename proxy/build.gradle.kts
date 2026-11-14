plugins {
    id("java-library")
    id("xyz.jpenilla.run-velocity") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    implementation(project(":protocol"))
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.jar {
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks {
    runVelocity {
        velocityVersion("3.4.0-SNAPSHOT")
    }
}