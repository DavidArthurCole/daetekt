import com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("packaging")
    kotlin("jvm")
    id("jacoco")
}

val versionCatalog = versionCatalogs.named("libs")
val jvmMajorVersion = versionCatalog.findVersion("jvmTarget").get().requiredVersion.toInt()

jacoco.toolVersion = versionCatalog.findVersion("jacoco").get().requiredVersion

tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    systemProperty("junit.jupiter.testinstance.lifecycle.default", "per_class")
    systemProperty("junit.platform.discovery.issue.severity.critical", "INFO")
    val compileTestSnippets = providers.gradleProperty("compile-test-snippets").orNull.toBoolean()
    systemProperty("compile-test-snippets", compileTestSnippets)

    maxHeapSize = "3g"

    testLogging {
        // set options for log level LIFECYCLE
        events = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.STANDARD_ERROR,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.SKIPPED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    configure<JacocoTaskExtension> {
        excludes = listOf("org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors")
    }

    configure<DevelocityTestConfiguration> {
        testRetry {
            @Suppress("MagicNumber")
            if (providers.environmentVariable("CI").isPresent) {
                maxRetries = 3
                maxFailures = 20
            }
        }
        predictiveTestSelection {
            enabled = providers.gradleProperty("enablePTS").map(String::toBooleanStrict)
        }
    }
}

kotlin {
    jvmToolchain(versionCatalog.findVersion("jdk").get().requiredVersion.toInt())

    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(jvmMajorVersion.toString())

        extraWarnings = true
        allWarningsAsErrors = providers.gradleProperty("warningsAsErrors").orNull.toBoolean()
        freeCompilerArgs.add(jvmTarget.map { "-Xjdk-release=${it.target}" })

        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        freeCompilerArgs.add("-Xreturn-value-checker=check")

        if (project.name != "detekt-gradle-plugin") {
            // Only enable progressive mode in non-DGP modules. DGP doesn't compile with latest language version so
            // progressive mode is not appropriate.
            progressiveMode = true
        }
    }
}

testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(versionCatalog.findVersion("junit").get().requiredVersion)
        }
        register<JvmTestSuite>("multiJvmTest") {
            targets {
                @Suppress("MagicNumber")
                setOf(17, 21, 25).forEach {
                    register("testJvm$it") {
                        testTask {
                            javaLauncher = javaToolchains.launcherFor {
                                languageVersion = JavaLanguageVersion.of(it)
                            }
                            classpath = tasks.test.get().classpath
                            testClassesDirs = tasks.test.get().testClassesDirs
                        }
                    }
                }
            }
        }
    }
}

java {
    withSourcesJar()
    if (project.name !in setOf("detekt-gradle-plugin", "detekt-test-junit")) {
        // DGP uses different versions of kotlin-gradle-api in test runtime and compile time
        consistentResolution {
            useCompileClasspathVersions()
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = jvmMajorVersion
}
