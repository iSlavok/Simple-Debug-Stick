import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.17.17"
    kotlin("jvm") version "2.4.10"
}

val javaLevel = 21
val minecraftDep = ">=1.21.5 <1.21.9"

version = "${property("mod_version")}+mc${property("minecraft_version")}"
group = property("maven_group") as String

base { archivesName.set(property("archives_base_name") as String) }

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Recipe + advancement JSON are generated into src/main/generated, which Loom adds
// as a resource root. The generated files are committed so a plain `./gradlew build`
// (and CI) ships the recipe without having to run datagen first.
fabricApi {
    configureDataGeneration()

    // Headless in-game tests (see src/gametest). Booting the server at all proves the
    // mixins apply; the tests themselves cover the datapack recipe.
    configureTests {
        createSourceSet = true
        modId = "${property("archives_base_name")}-test"
        eula = true
    }
}

dependencies {
    "modGametestImplementation"("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    "modGametestImplementation"("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "java_level" to javaLevel,
        "minecraft_dep" to minecraftDep,
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "*.mixins.json")) { expand(props) }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(javaLevel) }

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(javaLevel.toString()))
}

java {
    withSourcesJar()
    val jv = JavaVersion.toVersion(javaLevel)
    sourceCompatibility = jv
    targetCompatibility = jv
}

tasks.jar {
    from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } }
}
