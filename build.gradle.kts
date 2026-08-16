import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "io.github.lijinhong11"
version = property("version")!! as String

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.1")
    implementation("io.github.lijinhong11:tiptap-markdown-java:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
    testImplementation(gradleTestKit())
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

gradlePlugin {
    website.set("https://github.com/lijinhong11/NexusMCPublisher")
    vcsUrl.set("https://github.com/lijinhong11/NexusMCPublisher.git")

    plugins {
        create("nexusMCPublisher") {
            id = "io.github.lijinhong11.nexusmcpublisher"
            implementationClass = "io.github.lijinhong11.nexusmcpublisher.NexusMCPublisherPlugin"
            displayName = "NexusMC Publisher"
            description = "Uploads and publishes resources to NexusMC"
            tags.set(listOf("nexusmc", "publishing", "minecraft", "automation"))

            compatibility {
                features {
                    configurationCache = false
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}