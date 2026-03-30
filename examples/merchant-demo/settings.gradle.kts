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
    }
}

rootProject.name = "MerchantDemo"
include(":app")

// Include the SDK from the repo root (two levels up from examples/merchant-demo/)
includeBuild("../../") {
    dependencySubstitution {
        substitute(module("com.monei.pay:sdk")).using(project(":sdk"))
    }
}
