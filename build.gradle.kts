import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

// Compact distribution copies only; keep editable source assets formatted.
class CompactJsonReader(reader: Reader) : FilterReader(
    StringReader(JsonOutput.toJson(JsonSlurper().parse(reader)))
)

val minecraftVersion = stonecutter.current.version
val modId = project.property("modId") as String
val modVersion = project.property("modVersion") as String
val javaVersion = project.property("javaVersion") as String
val fabricLoaderVersion = project.property("fabricLoaderVersion") as String
val architecturyApiVersion = project.property("architecturyApiVersion") as String
val resourcePackFormat = project.property("resourcePackFormat") as String

version = "$modVersion+$minecraftVersion"
base {
    archivesName.set("$modId-common")
}

architectury.common(stonecutter.tree.branches.mapNotNull { branch ->
    if (stonecutter.current.project !in branch) null
    else branch.project.findProperty("loom.platform") as String?
})

loom {
    silentMojangMappingsLicense()
    mixin {
        useLegacyMixinAp.set(true)
        defaultRefmapName.set("craftdistrict.refmap.json")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("dev.architectury:architectury:$architecturyApiVersion")
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
    sourceCompatibility = JavaVersion.toVersion(javaVersion.toInt())
    targetCompatibility = JavaVersion.toVersion(javaVersion.toInt())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.toInt())
}

tasks.processResources {
    filteringCharset = "UTF-8"
    // Carry the unmodified project license into every platform's common bundle.
    from(rootProject.file("LICENSE"))
    inputs.property("resource_pack_format", resourcePackFormat)
    inputs.property("mixin_java_version", "JAVA_$javaVersion")
    filesMatching("pack.mcmeta") {
        expand("resource_pack_format" to resourcePackFormat)
    }
    filesMatching("craftdistrict.mixins.json") {
        expand("mixin_java_version" to "JAVA_$javaVersion")
    }
    filesMatching(listOf("assets/**/*.json", "data/**/*.json")) {
        filter(CompactJsonReader::class.java)
    }
    if (minecraftVersion == "1.21.1") {
        eachFile {
            if (path.startsWith("data/craftdistrict/loot_tables/")) {
                path = path.replace("data/craftdistrict/loot_tables/", "data/craftdistrict/loot_table/")
            }
            if (path.startsWith("data/minecraft/tags/blocks/")) {
                path = path.replace("data/minecraft/tags/blocks/", "data/minecraft/tags/block/")
            }
        }
    }
}
