plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "demo.Client"
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}