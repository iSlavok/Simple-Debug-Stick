import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Versioned build script — applied once per Stonecutter node (yarn / obfuscated
// anchors). The unobfuscated 26.x nodes use build.unobfuscated.gradle.kts instead.
plugins {
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.17.17"
    kotlin("jvm") version "2.4.10"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

// Everything that differs between Minecraft versions lives here; the shared source
// in src/ only branches where the touched API actually changes.
data class Mc(
    val yarn: String,
    val flk: String,          // fabric-language-kotlin (runtime language adapter)
    val fapi: String,         // fabric-api
    val java: Int,
    val depends: String,      // "minecraft" range for fabric.mod.json
    val gameVersions: List<String>,
    // Recipe JSON era -> src/main/recipes/<era> is added to the resources.
    val recipeEra: String,
    // In-game gametests need a compatible fabric-gametest-api-v1; enable only where
    // this anchor's fabric-api ships one. Older anchors rely on the unit tests.
    val gametest: Boolean = false,
)

val mcVersion = stonecutter.current.version
val mc = when (mcVersion) {
    "1.18.2" -> Mc(
        yarn = "1.18.2+build.4", flk = "1.12.3+kotlin.2.0.21", fapi = "0.77.0+1.18.2",
        java = 17, depends = ">=1.18 <1.19",
        gameVersions = listOf("1.18", "1.18.1", "1.18.2"), recipeEra = "legacy",
    )
    "1.19.4" -> Mc(
        yarn = "1.19.4+build.2", flk = "1.12.3+kotlin.2.0.21", fapi = "0.87.2+1.19.4",
        java = 17, depends = ">=1.19 <1.20",
        gameVersions = listOf("1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4"),
        recipeEra = "legacy",
    )
    "1.20.4" -> Mc(
        yarn = "1.20.4+build.3", flk = "1.12.3+kotlin.2.0.21", fapi = "0.97.3+1.20.4",
        java = 17, depends = ">=1.20 <1.20.5",
        gameVersions = listOf("1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4"),
        recipeEra = "transform-old",
    )
    "1.20.6" -> Mc(
        yarn = "1.20.6+build.3", flk = "1.13.13+kotlin.2.4.10", fapi = "0.100.8+1.20.6",
        java = 21, depends = ">=1.20.5 <1.21",
        gameVersions = listOf("1.20.5", "1.20.6"), recipeEra = "transform-mid",
        gametest = true,
    )
    "1.21.1" -> Mc(
        yarn = "1.21.1+build.3", flk = "1.13.13+kotlin.2.4.10", fapi = "0.116.14+1.21.1",
        java = 21, depends = ">=1.21 <1.21.2",
        gameVersions = listOf("1.21", "1.21.1"), recipeEra = "transform-obj",
        gametest = true,
    )
    "1.21.8" -> Mc(
        yarn = "1.21.8+build.1", flk = "1.13.13+kotlin.2.4.10", fapi = "0.136.1+1.21.8",
        java = 21, depends = ">=1.21.2 <1.21.9",
        gameVersions = listOf("1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8"),
        recipeEra = "transform-new", gametest = true,
    )
    "1.21.10" -> Mc(
        yarn = "1.21.10+build.2", flk = "1.13.13+kotlin.2.4.10", fapi = "0.138.3+1.21.10",
        java = 21, depends = ">=1.21.9 <1.22",
        gameVersions = listOf("1.21.9", "1.21.10", "1.21.11"), recipeEra = "transform-new",
        gametest = true,
    )
    else -> error("Unconfigured Minecraft version: $mcVersion")
}

version = "${property("mod_version")}+mc$mcVersion"
group = property("maven_group") as String
base { archivesName.set(property("archives_base_name") as String) }

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:${mc.yarn}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${mc.fapi}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${mc.flk}")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Hand-authored recipe JSON, one dir per format era, added as a resource root.
sourceSets.main {
    resources.srcDir("src/main/recipes/${mc.recipeEra}")
}

// In-game gametest (booted server): asserts the recipe loads and, by booting at all,
// that the required mixin applied. Only on anchors with a compatible gametest API.
if (mc.gametest) {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = "${property("archives_base_name")}-test"
            eula = true
        }
    }
    dependencies {
        "modGametestImplementation"("net.fabricmc.fabric-api:fabric-api:${mc.fapi}")
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "java_level" to mc.java,
        "minecraft_dep" to mc.depends,
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "*.mixins.json")) { expand(props) }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(mc.java) }

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(mc.java.toString()))
        // Pin the language/API level to the oldest fabric-language-kotlin we ship
        // against (kotlin 2.0), so compiled code never references a stdlib symbol
        // missing from an older anchor's bundled Kotlin runtime.
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

java {
    withSourcesJar()
    val jv = JavaVersion.toVersion(mc.java)
    sourceCompatibility = jv
    targetCompatibility = jv
}

tasks.jar {
    from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } }
}

publishMods {
    file.set(tasks.named<AbstractArchiveTask>("remapJar").flatMap { it.archiveFile })
    version.set(project.version.toString())
    type.set(me.modmuss50.mpp.ReleaseType.STABLE)
    modLoaders.add("fabric")
    modLoaders.add("quilt")
    changelog.set("See https://github.com/iSlavok/Simple-Debug-Stick/releases")
    displayName.set("Simple Debug Stick ${property("mod_version")} (Fabric/Quilt, MC $mcVersion)")
    modrinth {
        projectId.set(providers.gradleProperty("modrinth_id"))
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.addAll(mc.gameVersions)
        requires("fabric-api")
        requires("fabric-language-kotlin")
    }
}
