package io.tiangou

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.tiangou.DrawImageApiEnum.AWT
import io.tiangou.DrawImageApiEnum.SKIKO
import io.tiangou.other.http.client
import io.tiangou.other.http.isRedirect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.utils.MiraiLogger
import org.jetbrains.skiko.Library
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostId
import org.jetbrains.skiko.hostOs
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.*

abstract class LibrariesLoader {

    protected val log: MiraiLogger = MiraiLogger.Factory.create(this::class)

    abstract val libDescription: String

    protected val libPath
        get() = Global.drawImageConfig.libDictionary

    suspend fun loadLibraries(block: Path.() -> Unit) {
        log.info("loading $libDescription libraries...")
        block(getLibrariesPath())
        log.info("$libDescription libraries loaded")
    }

    fun Path.create(block: Path.() -> Unit): Path {
        if (!exists()) {
            if (!parent.exists()) parent.createDirectories()
            createFile()
            block()
        }
        return this
    }

    abstract suspend fun getLibrariesPath(): Path


    companion object {

        suspend fun loadApi(apiEnum: DrawImageApiEnum) {
            when (apiEnum) {
                SKIKO -> SkikoLibrariesLoader.loadLibraries {
                    System.setProperty("skiko.library.path", absolutePathString())
                    Library.load()
                }

                AWT -> {}
            }
        }

    }

}


object SkikoLibrariesLoader : LibrariesLoader() {

    private val packageSequence: Sequence<String> = sequenceOf("org", "jetbrains", "skiko")

    private val target = "skiko-awt-runtime-$hostId"

    private const val version = "0.7.9"

    private val downloadUrl = "https://maven.pkg.jetbrains.space/public/p/compose/dev" +
            packageSequence.joinToString("/", prefix = "/", postfix = "/") +
            "$target/" +
            "$version/" +
            "$target-$version.jar"

    override val libDescription: String = "skiko"

    override suspend fun getLibrariesPath(): Path {
        val path = libPath.resolve(packageSequence.joinToString(File.separator))
            .resolve(target)
            .resolve(version)

        val nativeLibName = "skiko-$hostId"

        fun ZipFile.unzipTo(entry: ZipEntry, path: Path) =
            getInputStream(entry).use { path.writeBytes(it.readBytes()) }

        val zipPath = path.resolve("$target-$version.jar")
            .create {
                log.info("$libDescription is not exists, starting download...")
                log.info("$libDescription download path: $this")
                runBlocking(Dispatchers.IO) {
                    client.get(downloadUrl).apply {
                        if (status.isRedirect()) {
                            writeBytes(client.get(headers["location"]!!).readBytes())
                        } else writeBytes(readBytes())
                    }

                }
                log.info("$libDescription downloaded")
            }

        path.resolve(System.mapLibraryName(nativeLibName))
            .create {
                runBlocking(Dispatchers.IO) {
                    val zip = ZipFile(zipPath.toFile())
                    zip.unzipTo(zip.getEntry(System.mapLibraryName("skiko-$hostId")), this@create)
                    log.info("$libDescription: $nativeLibName unzipped")
                }
            }

        if (hostOs == OS.Windows) {
            path.resolve("icudtl.dat")
                .create {
                    runBlocking(Dispatchers.IO) {
                        val zip = ZipFile(zipPath.toFile())
                        zip.unzipTo(zip.getEntry("icudtl.dat"), this@create)
                        log.info("$libDescription: icudtl.dat unzipped")
                    }
                }
        }

        return path

    }


}