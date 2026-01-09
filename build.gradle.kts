plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.hiusers.klos"
version = "0.0.4"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:8.1.1")
}

gradlePlugin {
    plugins {
        create("klos") {
            id = "com.hiusers.klos"
            implementationClass = "com.hiusers.klos.KlosPlugin"
            displayName = "Klos ORM Helper"
            description = "Auto configures Kotlin and Exposed dependencies in plugin.yml for TabooLib projects"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://repo.hiusers.com/releases")
            credentials {
                username = project.findProperty("mavenUsername")?.toString() 
                    ?: project.findProperty("MAVEN_USERNAME")?.toString()
                    ?: System.getenv("MAVEN_USERNAME")
                password = project.findProperty("mavenPassword")?.toString() 
                    ?: project.findProperty("MAVEN_PASSWORD")?.toString()
                    ?: System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
