plugins {
    val kotlinVersion = "1.8.22"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "io.tiangou"
version = "0.8.2"

val ktorVersion = "2.3.4"
val skikoVersion = "0.7.80"
val sqliteJdbcVersion = "3.42.0.0"
val hutoolVersion = "5.8.20"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}


dependencies {
    implementation("org.xerial", "sqlite-jdbc", sqliteJdbcVersion)
    implementation("cn.hutool", "hutool-cron", hutoolVersion)
    implementation("io.ktor", "ktor-client-core", ktorVersion)
    implementation("io.ktor", "ktor-client-okhttp", ktorVersion)
    implementation("io.ktor", "ktor-client-content-negotiation", ktorVersion)
    implementation("io.ktor", "ktor-serialization-kotlinx-json", ktorVersion)
    implementation("io.ktor", "ktor-client-logging", ktorVersion)
    implementation("io.ktor", "ktor-client-encoding", ktorVersion)
    // skiko support
    implementation("org.jetbrains.skiko", "skiko-awt", skikoVersion)

}