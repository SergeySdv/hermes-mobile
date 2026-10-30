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
        google()
        mavenCentral()
        maven("https://maven.rokid.com/repository/maven-public/") {
            content { includeGroupByRegex("com\\.rokid.*") }
        }
    }
}
rootProject.name = "HermesRokidS3"
include(":addon", ":companion")
