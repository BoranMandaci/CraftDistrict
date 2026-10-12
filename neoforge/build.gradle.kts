plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val minecraftVersion = stonecutter.current.version
val common = requireNotNull(stonecutter.node?.sibling("")?.project) {
    "No common project for $project"
}
val modId = project.property("modId") as String
val modVersion = project.property("modVersion") as String

version = "$modVersion+$minecraftVersion"
base {
    archivesName.set("$modId-neoforge")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

val commonBundle = configurations.create("commonBundle") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val shadowBundle = configurations.create("shadowBundle") {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    named("developmentNeoForge").get().extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

val neoforgeVersion = common.property("neoforgeVersion") as String
val architecturyApiVersion = common.property("architecturyApiVersion") as String

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    "neoForge"("net.neoforged:neoforge:$neoforgeVersion")
    modImplementation("dev.architectury:architectury-neoforge:$architecturyApiVersion")
    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionNeoForge")) { isTransitive = false }
}

java {
    withSourcesJar()
    val javaVersion = (common.property("javaVersion") as String).toInt()
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set((common.property("javaVersion") as String).toInt())
}

tasks.processResources {
    val modName = project.property("modName") as String
    val modDescription = project.property("modDescription") as String
    val modLicense = project.property("modLicense") as String
    val properties = mapOf(
        "mod_id" to modId,
        "mod_version" to modVersion,
        "mod_name" to modName,
        "mod_description" to modDescription,
        "mod_license" to modLicense,
        "minecraft_version" to minecraftVersion,
    )
    inputs.properties(properties)
    filesMatching("META-INF/neoforge.mods.toml") { expand(properties) }
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier.set("dev-shadow")
    exclude("fabric.mod.json", "architectury.common.json")
}
tasks.remapJar {
    input.set(tasks.shadowJar.flatMap { it.archiveFile })
    dependsOn(tasks.shadowJar)
}
tasks.jar {
    archiveClassifier.set("dev")
}
