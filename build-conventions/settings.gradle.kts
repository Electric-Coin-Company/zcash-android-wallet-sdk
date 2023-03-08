pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        val toolchainResolverPluginVersion = extra["FOOJAY_TOOLCHAIN_RESOLVER_VERSION"].toString()
        id("org.gradle.toolchains.foojay-resolver-convention") version(toolchainResolverPluginVersion) apply(false)
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
