plugins {
    val kotlinVersion = "1.8.22"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "io.tiangou.valobot"
version = "0.6.1"

val kotlinVersion = "1.8.20"
val ktorVersion = "2.2.4"
val seleniumVersion = "4.9.1"
val freemarkerVersion = "2.3.32"
val skikoVersion = "0.7.9"
val serializationVersion = "1.5.1"

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenLocal()
}


dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("cn.hutool:hutool-cron:5.8.20")
    // skiko support
    implementation("org.jetbrains.skiko:skiko-awt:$skikoVersion")
}