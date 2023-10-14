package io.tiangou

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.config.PluginConfig
import io.tiangou.other.http.client
import io.tiangou.other.http.isRedirect
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.utils.MiraiLogger
import org.jetbrains.skiko.*
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.*

abstract class GuiLibraryLoader {

    protected val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

    abstract val libDescription: String

    protected val libPath
        get() = PluginConfig.drawImageConfig.libDictionary

    suspend fun loadLibraries(block: Path.() -> Unit) {
        log.info("loading $libDescription library...")
        block(getLibraryPath())
        log.info("$libDescription library loaded")
    }

    fun Path.create(block: suspend Path.() -> Unit): Path {
        if (!exists()) {
            if (!parent.exists()) parent.createDirectories()
            createFile()
            runBlocking { block() }
        }
        return this
    }

    abstract suspend fun getLibraryPath(): Path


    companion object {

        suspend fun loadApi(apiEnum: PluginConfig.DrawImageConfig.DrawImageApiEnum) {
            when (apiEnum) {
                PluginConfig.DrawImageConfig.DrawImageApiEnum.SKIKO -> SkikoLibraryLoader.loadLibraries {
                    System.setProperty("skiko.library.path", absolutePathString())
                    Library.load()
                }

                PluginConfig.DrawImageConfig.DrawImageApiEnum.AWT -> {}
            }
        }

    }

}


object SkikoLibraryLoader : GuiLibraryLoader() {

    private val baseDirs: Array<String> = arrayOf("org", "jetbrains", "skiko")

    private val target = "skiko-awt-runtime-$hostId"

    private val downloadUrl = "https://maven.pkg.jetbrains.space/public/p/compose/dev" +
            "/${baseDirs.joinToString("/")}" +
            "/$target/${Version.skiko}/$target-${Version.skiko}.jar"

    override val libDescription: String = "skiko"

    override suspend fun getLibraryPath(): Path {
        val libraryPath = libPath.resolve(baseDirs.joinToString(File.separator))
            .resolve(target)
            .resolve(Version.skiko)
        val nativeLibName = "skiko-$hostId"
        fun File.toZipFile() = ZipFile(this)
        fun ZipFile.unzipTo(entry: ZipEntry, path: Path) = getInputStream(entry).use { path.writeBytes(it.readBytes()) }


        val zipFile = libraryPath.resolve("$target-${Version.skiko}.jar")
            .create {
                log.info("$libDescription is not exists, starting download...")
                log.info("$libDescription download path: $this")
                val bytes = client.get(downloadUrl).run {
                    takeIf { !it.status.isRedirect() } ?: client.get(headers["location"]!!)
                }.readBytes()
                writeBytes(bytes)
                log.info("$libDescription downloaded")
            }.toFile().toZipFile()

        libraryPath.resolve(System.mapLibraryName(nativeLibName))
            .create {
                zipFile.unzipTo(zipFile.getEntry(System.mapLibraryName("skiko-$hostId")), this@create)
                log.info("$libDescription: $nativeLibName unzipped")
            }

        if (hostOs == OS.Windows) {
            libraryPath.resolve("icudtl.dat")
                .create {
                    zipFile.unzipTo(zipFile.getEntry("icudtl.dat"), this@create)
                    log.info("$libDescription: icudtl.dat unzipped")
                }
        }

        return libraryPath

    }


}