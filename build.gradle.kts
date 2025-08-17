plugins {
    kotlin("jvm") version "2.2.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.tonikelope"
version = "8.23"

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.cloudsmith.io/public/olivier-ayache/xuggler/maven/")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.tonikelope.megabasterd.MainPanel")
}

sourceSets {
    val main by getting {
        java.srcDirs("src/main/java", "src/main/kotlin")
    }
    val test by getting {
        java.srcDirs("src/test/java")
    }
}

dependencies {
    implementation("commons-io:commons-io:2.14.0")
    implementation("org.sejda.webp-imageio:webp-imageio-sejda:0.1.0")
    implementation("org.xerial:sqlite-jdbc:3.43.0.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.3")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("xuggle:xuggle-xuggler-server-all:5.7.0-SNAPSHOT")
}

tasks.shadowJar {
    dependsOn(tasks.jar)
    archiveClassifier.set("")
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "SplashScreen-Image" to "images/dot_com.png"
        )
    }
}

tasks {
    named<Zip>("distZip") {
        dependsOn(shadowJar)
    }

    named<Tar>("distTar") {
        dependsOn(shadowJar)
    }

    named<CreateStartScripts>("startShadowScripts") {
        dependsOn(shadowJar)
    }

    named<CreateStartScripts>("startScripts") {
        dependsOn(shadowJar)
        classpath = files(shadowJar.flatMap { it.archiveFile })
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
