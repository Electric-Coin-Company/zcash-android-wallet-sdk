pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        val toolchainResolverPluginVersion = extra["FOOJAY_TOOLCHAIN_RESOLVER_VERSION"].toString()
        id("org.gradle.toolchains.foojay-resolver-convention") version(toolchainResolverPluginVersion) apply(false)
        id("com.vanniktech.maven.publish") version "0.32.0" apply false
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "build-conventions"
