pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 本机 maven.google.com 直连超时，但 dl.google.com 的 maven2 镜像可达；
        // google() 在 Gradle 中即解析到 dl.google.com/dl/android/maven2/
        google()
        mavenCentral()
    }
}

rootProject.name = "TingKe"
include(":app")
