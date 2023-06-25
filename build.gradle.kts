import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

plugins {
    val kotlinVersion = "1.8.22"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("net.mamoe.mirai-console") version "2.14.0"
}

enum class SkikoOSArch(
    val os: String,
    val arch: String
) {

    WINDOWS_X64("windows", "x64"),
    MACOS_X64("macos", "x64"),
    LINUX_X64("linux", "x64"),
    WINDOWS_ARM64("windows", "arm64"),
    MACOS_ARM64("macos", "arm64"),
    LINUX_ARM64("linux", "arm64"),

}

group = "io.tiangou.valobot"
version = "0.4.0"

val kotlinVersion = "1.8.20"
val ktorVersion = "2.2.4"
val seleniumVersion = "4.9.1"
val freemarkerVersion = "2.3.32"
val skikoVersion = "0.7.58"
val serializationVersion = "1.5.1"
// jar目标运行环境
val skikoOsArch = SkikoOSArch.LINUX_X64

repositories {
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenLocal()
}

mirai {
    base.archivesName.set(base.archivesName.get() + "_" + skikoOsArch.name.toLowerCaseAsciiOnly())
}


dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$serializationVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("cn.hutool:hutool-cron:5.8.20")
    // skiko support
    compileOnly("org.jetbrains.skiko:skiko-awt:$skikoVersion")
    runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-${skikoOsArch.os}-${skikoOsArch.arch}:$skikoVersion")
}