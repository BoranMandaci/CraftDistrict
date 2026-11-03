import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.Deflater
import org.apache.tools.zip.ZipEntry
import java.util.zip.ZipFile
import org.apache.tools.zip.ZipOutputStream
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.17-SNAPSHOT" apply false
    id("architectury-plugin") version "3.5-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "9.4.3" apply false
}

stonecutter active "1.21.1" /* [SC] DO NOT EDIT */

abstract class ReleaseWorkers @Inject constructor(val executor: WorkerExecutor)

// Recompress production archives only. Entry contents, paths and manifests stay intact.
fun compressReleaseJar(archive: java.io.File) {
    val temporary = Files.createTempFile(archive.toPath().parent, archive.name, ".tmp")
    try {
        ZipFile(archive).use { source ->
            ZipOutputStream(temporary.toFile()).use { output ->
                output.setLevel(Deflater.BEST_COMPRESSION)
                source.entries().asSequence().forEach { entry ->
                    val copy = ZipEntry(entry.name).apply {
                        time = entry.time
                        size = entry.size
                        comment = entry.comment
                        entry.extra?.let { extra = it }
                    }
                    output.putNextEntry(copy)
                    if (!entry.isDirectory) source.getInputStream(entry).use { it.copyTo(output) }
                    output.closeEntry()
                }
            }
        }
        if (Files.size(temporary) < archive.length()) {
            Files.move(temporary, archive.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(temporary)
    }
}

subprojects {
    tasks.withType<RemapJarTask>().configureEach {
        val workers = project.objects.newInstance(ReleaseWorkers::class.java)
        doLast {
            // Loom submits asynchronous work; finish it before touching its output.
            workers.executor.await()
            val archive = archiveFile.get().asFile
            val originalSize = archive.length()
            compressReleaseJar(archive)
            logger.lifecycle("${archive.name}: $originalSize -> ${archive.length()} bytes")
        }
    }
}

tasks.register("chiseledBuild") {
    group = "build"
    description = "Builds all configured Minecraft version and loader combinations."
    dependsOn(stonecutter.tasks.named("build").map { it.values })
}
