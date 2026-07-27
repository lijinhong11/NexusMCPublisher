plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.1.1"
}

group = "io.github.lijinhong11"
version = providers.gradleProperty("pluginVersion").getOrElse("1.0.0-SNAPSHOT")

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.4")

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
            id = "io.github.lijinhong11.nexusmc-publisher"
            implementationClass = "io.github.lijinhong11.nexusmcpublisher.NexusMCPublisherPlugin"
            displayName = "NexusMC Publisher"
            description = "Uploads and publishes resources to NexusMC"
            tags.set(listOf("nexusmc", "publishing", "minecraft", "automation"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}