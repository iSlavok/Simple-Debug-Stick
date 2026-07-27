import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Build script for the unobfuscated era (Minecraft 26+). No Yarn mappings exist; the
// game ships with Mojang names, so this uses the non-remapping Loom variant.
plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17.17" // non-remapping variant
    kotlin("jvm") version "2.4.10"
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

data class Unobf(
    val fapi: String,
    val flk: String,
    val runtimeJava: Int,     // Java the game requires at runtime (fabric.mod.json + mixin level)
    val depends: String,
    val gameVersions: List<String>,
    val recipeEra: String,
)

// Our bytecode targets 21; Java 21 classes run fine on the game's Java 25 runtime.
// The mixin compatibility level must match the game's class version, so that uses
// runtimeJava.
val compileJava = 21

val mcVersion = stonecutter.current.version
val u = when (mcVersion) {
    "26.1.2" -> Unobf(
        fapi = "0.155.2+26.1.2", flk = "1.13.13+kotlin.2.4.10", runtimeJava = 25,
        depends = ">=26.1 <26.2", gameVersions = listOf("26.1", "26.1.1", "26.1.2"),
        recipeEra = "transform-new",
    )
    "26.2" -> Unobf(
        fapi = "0.155.2+26.2", flk = "1.13.13+kotlin.2.4.10", runtimeJava = 25,
        depends = ">=26.2 <27", gameVersions = listOf("26.2"),
        recipeEra = "transform-new",
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
    // No mappings() — unobfuscated.
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${u.fapi}")
    // Runtime-only language adapter: our code never references its classes, but the
    // mod depends on it in fabric.mod.json, so the dev client/server needs it on the
    // runtime classpath. Players supply it themselves at install time.
    runtimeOnly("net.fabricmc:fabric-language-kotlin:${u.flk}")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

sourceSets.main {
    resources.srcDir("src/main/recipes/${u.recipeEra}")
}

// In-game gametest on the non-remapping toolchain. It is apply-only here (the registry
// API is Mojang-mapped), but booting proves the required Mojang-named mixin applied —
// which compile-time cannot check without remapping. Runs on the JDK 25 daemon.
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "${property("archives_base_name")}-test"
        eula = true
    }
}
dependencies {
    "gametestImplementation"("net.fabricmc.fabric-api:fabric-api:${u.fapi}")
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "java_level" to u.runtimeJava,
        "minecraft_dep" to u.depends,
    )
    inputs.properties(props)
    filesMatching(listOf("fabric.mod.json", "*.mixins.json")) { expand(props) }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(compileJava) }

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(compileJava.toString()))
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

java {
    withSourcesJar()
    val jv = JavaVersion.toVersion(compileJava)
    sourceCompatibility = jv
    targetCompatibility = jv
}

tasks.jar {
    from("LICENSE") { rename { "${it}_${base.archivesName.get()}" } }
}

// Non-remapping build: publish the plain `jar` (there is no remapJar here).
publishMods {
    file.set(tasks.named<AbstractArchiveTask>("jar").flatMap { it.archiveFile })
    version.set(project.version.toString())
    type.set(me.modmuss50.mpp.ReleaseType.STABLE)
    modLoaders.add("fabric")
    modLoaders.add("quilt")
    changelog.set("See https://github.com/iSlavok/Simple-Debug-Stick/releases")
    displayName.set("Simple Debug Stick ${property("mod_version")} (Fabric/Quilt, MC $mcVersion)")
    modrinth {
        projectId.set(providers.gradleProperty("modrinth_id"))
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.addAll(u.gameVersions)
        requires("fabric-api")
        requires("fabric-language-kotlin")
    }
}
