plugins {
    val kotlinVersion = "1.8.22"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "io.tiangou.plugins"
version = "0.7.0"

val ktorVersion = "2.2.4"
val skikoVersion = "0.7.72"
val sqliteJdbcVersion = "3.42.0.0"
val hutoolVersion = "5.8.20"

mirai {
    jvmTarget = JavaVersion.VERSION_1_8
    consoleVersion = "2.14.0"
}

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenLocal()
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