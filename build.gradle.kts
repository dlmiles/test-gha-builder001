//
//
// SPDX-FileCopyrightText: Copyright 2023 Darryl L. Miles
// SPDX-License-Identifier: Apache2.0
//
//
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.FileOutputStream

plugins {
    application
    java
    kotlin("jvm") version "1.9.23" // kotlinVersion
    id("org.graalvm.buildtools.native") version "0.10.2"
}

group = "org.darrylmiles"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    val com_fasterxml_jackson__jackson_bom: String by project
    val org_yaml__snakeyaml: String by project
    val info_picocli: String by project
    val org_jline: String by project

    testImplementation(kotlin("test"))

    implementation(kotlin("stdlib-jdk8"))

    implementation("com.fasterxml.jackson.core:jackson-databind:${com_fasterxml_jackson__jackson_bom}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${com_fasterxml_jackson__jackson_bom}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:${com_fasterxml_jackson__jackson_bom}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:${com_fasterxml_jackson__jackson_bom}")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:${com_fasterxml_jackson__jackson_bom}")

    implementation("org.yaml:snakeyaml:${org_yaml__snakeyaml}")
    implementation("info.picocli:picocli:${info_picocli}")

    compileOnly("info.picocli:picocli-codegen:${info_picocli}")
    //compileOnly("info.picocli:picocli-jansi-graalvm:${info_picocli}")

//    implementation("org.fusesource.jansi:jansi:2.4.1")
    implementation("org.jline:jansi:${org_jline}")
//    implementation("org.jline:jline-terminal-jna:${org_jline}")
//    implementation("org.jline:jline-terminal-jansi:${org_jline}")

    // JNI windows
    // JNI linux
    // JNI macosx
}


tasks.register("generatePicocliReflectJson", Exec::class.java) {
    group = "generation"
    description = "Create GraalVM reflect.json for picocli"

    val pathToPicocliJar = project.configurations.compileClasspath.get()
        .filter { it.name.startsWith("picocli-") }
        .map { it.absolutePath }
        .toTypedArray()
    val pathTtKotlinJar = project.configurations.compileClasspath.get()
        .filter { it.name.startsWith("kotlin-") }
        .map { it.absolutePath }
        .toTypedArray()

    val cp = listOf(
        *pathToPicocliJar,
        *pathTtKotlinJar,
        "build/classes/kotlin/main").joinToString(File.pathSeparator)

    logger.debug("classpath=$cp")

    commandLine("java",
        "-cp", cp,
        "picocli.codegen.aot.graalvm.ReflectionConfigGenerator",
        "org.darrylmiles.openlane.tooling.config.OL2Config")
    doFirst {
        standardOutput = FileOutputStream(project.file("build/generated/picocli-reflect.json"))
    }
    project.mkdir("build/generated")
    mustRunAfter(":compileKotlin")
}

graalvmNative {
    toolchainDetection = true

    binaries {
        named("main") {
            imageName = "ol2config"
            mainClass = "org.darrylmiles.openlane.tooling.config.OL2Config"
            buildArgs.addAll("-Ob",
                "-H:ReflectionConfigurationFiles=$buildDir/generated/picocli-reflect.json",
                "-H:+ReportUnsupportedElementsAtRuntime"
                )

            // -H:+BuildReport: Did not work with GHA GraalVM CE 21.0.2+13.1
            ////buildArgs.addAll("-H:+BuildReport")
            // --enable-sbom: Did not work with GHA GraalVM CE 21.0.2+13.1
            //buildArgs.addAll("--enable-sbom")
            //buildArgs.addAll("--pgo")

            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.GRAAL_VM)
            })
        }
        named("test") {
            buildArgs.add("-Ob")
        }
    }
    binaries.all {
        buildArgs.add("--verbose")
    }
}

tasks.findByName("nativeCompile")!!.dependsOn("generatePicocliReflectJson")

// FIXME write a task for upx use, detect if available
// provide task to run output through upx

tasks.withType(Jar::class.java).configureEach {
    manifest {
        attributes.putAll(mapOf(
            "Main-Class" to "org.darrylmiles.openlane.tooling.config.OL2Config",
            "Class-Path" to configurations.runtimeClasspath.get().fileCollection().joinToString(" ") { it.name }
            //.fileCollection().filter { it.name }.join(" ")
        ))
    }
}


val fatJar = tasks.register("fatJar", Jar::class) {
    group = "build"
    description = "Create all-in-one jar output"
    archiveClassifier = "uber"
    from(configurations.compileClasspath.get().map {
        if(it.isDirectory) it else zipTree(it)
    }) {
        exclude("META-INF/**")
        exclude("module-info.class")
    }
    from(layout.buildDirectory.dir("classes/kotlin/main"))
    from(layout.buildDirectory.dir("resources/main"))
    //from(configurations)
    dependsOn(":build")
}

val upx = tasks.register("upx", Exec::class) {
    group = "build"
    description = "Process native binary with UPX"
    val binaryFileExtn = if(Os.isFamily(Os.FAMILY_WINDOWS)) ".exe" else ""
    val binaryFilename = "ol2config${binaryFileExtn}"
    val nativeBinaryPath = layout.buildDirectory.file("native/nativeCompile/$binaryFilename")
    // FIXME detect if "upx" is available, emit details
    mkdir(layout.buildDirectory.dir("upx"))
    val upxOutputPath = layout.buildDirectory.file("upx/$binaryFilename")
    exec {
        commandLine("upx", "-7", "--best", "-v", "-o", upxOutputPath, nativeBinaryPath)
    }
    dependsOn(":nativeCompile")
}

application {
    mainClass = "org.darrylmiles.openlane.tooling.config.OL2Config"
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
