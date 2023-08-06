plugins {
    val kotlinVersion = "1.8.22"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "io.tiangou.plugins"
version = "0.7.0-dev"

val kotlinVersion = "1.8.20"
val ktorVersion = "2.2.4"
val seleniumVersion = "4.9.1"
val freemarkerVersion = "2.3.32"
val skikoVersion = "0.7.72"
val serializationVersion = "1.5.1"


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
    implementation("net.mamoe.yamlkt:yamlkt-jvm:0.12.0")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("cn.hutool:hutool-cron:5.8.20")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    // skiko support
    implementation("org.jetbrains.skiko:skiko-awt:$skikoVersion")

}