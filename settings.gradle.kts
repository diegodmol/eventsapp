pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.9.24"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Repositório oficial da Cielo para o SDK Smart POS (adicionar quando disponível
        // no ambiente de build real; aqui mantemos como referência documental).
        // maven { url = uri("https://repo.cielo.com.br/artifactory/smart-sdk") }
    }
}

rootProject.name = "EventTickets"

include(":app")
include(":core-ui")
include(":core-common")
include(":domain")
include(":data")
include(":feature-events")
include(":feature-checkout")
include(":feature-payment")
include(":feature-receipt")
