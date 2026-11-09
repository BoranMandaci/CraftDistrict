import groovy.json.JsonOutput
import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import java.io.FilterReader
import java.io.Reader
import java.io.StringReader
import java.math.BigDecimal

object DistributionJson {
    val writer: JsonGenerator = JsonGenerator.Options().disableUnicodeEscaping().build()
    private val vectors = setOf("from", "to", "origin", "uv", "translation", "rotation", "scale")

    private fun modelFloat(number: Number): Number {
        val value = number.toFloat()
        require(value.isFinite()) { "Non-finite model coordinate" }
        // Preserve signed zero; every other spelling round-trips to the same Minecraft float.
        if (value.toRawBits() == Int.MIN_VALUE) return value
        val decimal = BigDecimal(value.toString()).stripTrailingZeros()
        return if (decimal.scale() < 0) decimal.setScale(0) else decimal
    }

    @Suppress("UNCHECKED_CAST")
    fun model(value: Any?, key: String = ""): Any? = when (value) {
        is MutableMap<*, *> -> (value as MutableMap<String, Any?>).apply {
            keys.toList().forEach { name -> this[name] = model(this[name], name) }
            if (containsKey("from") && containsKey("to")) {
                remove("name") // Editor-only labels remain in the editable source models.
                if (get("shade") == true) remove("shade") // Vanilla's element default.
            }
            if (key == "rotation" && get("rescale") == false) remove("rescale")
            if (containsKey("texture") && (get("rotation") as? Number)?.toDouble() == 0.0) remove("rotation")
        }
        is List<*> -> if (key in vectors && value.all { it is Number }) {
            value.map { modelFloat(it as Number) }
        } else value.map { model(it, key) }
        else -> value
    }
}


plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

// Compact distribution copies only; keep editable source assets formatted.
class CompactJsonReader(reader: Reader) : FilterReader(
    StringReader(DistributionJson.writer.toJson(JsonSlurper().parse(reader)))
)

// Element names are editor metadata ignored by Minecraft; retain them in source.
@Suppress("UNCHECKED_CAST")
class CompactModelJsonReader(reader: Reader) : FilterReader(StringReader(
    DistributionJson.writer.toJson(DistributionJson.model(JsonSlurper().parse(reader)))
))

// Vanilla 1.21 renamed the ItemStack recipe-result key from item to id.
@Suppress("UNCHECKED_CAST")
class ModernRecipeJsonReader(reader: Reader) : FilterReader(StringReader(
    JsonOutput.toJson((JsonSlurper().parse(reader) as MutableMap<String, Any?>).apply {
        (get("result") as? MutableMap<String, Any?>)?.let { result ->
            result.remove("item")?.let { result["id"] = it }
        }
    })
))

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
    inputs.property("distribution_json_format", "minecraft-float32-utf8")
    inputs.property("lighting_effect_resources", 3)
    filesMatching("pack.mcmeta") {
        expand("resource_pack_format" to resourcePackFormat)
    }
    filesMatching("craftdistrict.mixins.json") {
        expand("mixin_java_version" to "JAVA_$javaVersion")
    }
    filesMatching(listOf("assets/**/*.json", "data/**/*.json")) {
        if (path.startsWith("assets/craftdistrict/models/")) {
            filter(CompactModelJsonReader::class.java)
        } else if (minecraftVersion == "1.21.1" && path.startsWith("data/craftdistrict/recipes/")) {
            filter(ModernRecipeJsonReader::class.java)
        } else {
            filter(CompactJsonReader::class.java)
        }
    }
    doLast {
        // Remove only the previous build-generated animations; models now use synchronized vanilla LIT states.
        val assets = destinationDir.resolve("assets/craftdistrict").canonicalFile
        for (path in listOf("models/block/lighting_effects", "textures/block/lighting_effects")) {
            val generated = assets.resolve(path).canonicalFile
            require(generated.toPath().startsWith(assets.toPath())
                && !rootProject.file("src/main/resources/assets/craftdistrict/" + path).exists())
            if (generated.exists()) check(generated.deleteRecursively())
        }
    }
    if (minecraftVersion == "1.21.1") {
        eachFile {
            if (path.startsWith("data/craftdistrict/loot_tables/")) {
                path = path.replace("data/craftdistrict/loot_tables/", "data/craftdistrict/loot_table/")
            }
            if (path.startsWith("data/craftdistrict/recipes/")) {
                path = path.replace("data/craftdistrict/recipes/", "data/craftdistrict/recipe/")
            }
            if (path.startsWith("data/craftdistrict/advancements/")) {
                path = path.replace("data/craftdistrict/advancements/", "data/craftdistrict/advancement/")
            }
            if (path.startsWith("data/minecraft/tags/blocks/")) {
                path = path.replace("data/minecraft/tags/blocks/", "data/minecraft/tags/block/")
            }
        }
    }
}
