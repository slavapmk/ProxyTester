plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.kotlin.kapt") version "1.6.0"
    application
}

group = "ru.slavapmk.proxychecker"
version = "0.1"

tasks.withType<Jar> {
    archiveFileName.set("${project.name}.jar")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

val exposedVersion: String = "0.44.0"

dependencies {
    implementation("com.squareup.retrofit2", "retrofit", "2.9.0")
    implementation("io.reactivex.rxjava3", "rxkotlin", "3.0.1")
    implementation("com.squareup.okhttp3:logging-interceptor:3.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:converter-gson:2.3.0")
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.9.0")
}

application {
    mainClass.set("ru.slavapmk.proxychecker.MainKt")
}