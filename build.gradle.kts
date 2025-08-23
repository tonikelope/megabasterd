plugins {
    kotlin("jvm") version "2.2.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.52.0"
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
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("com.tonikelope.megabasterd.MainPanel")
}

sourceSets {
    val main by getting {
        java.srcDirs("src/main/java", "src/main/kotlin")
    }
    val test by getting {
        java.srcDirs("src/test/java", "src/test/kotlin")
    }
}

dependencies {
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.25.1")
    implementation("org.apache.logging.log4j:log4j-api:2.25.1")
    implementation("org.apache.logging.log4j:log4j-core:2.25.1")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.0")
    implementation("org.swinglabs.swingx:swingx-all:1.6.5-1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    implementation("commons-io:commons-io:2.20.0")
    implementation("org.sejda.webp-imageio:webp-imageio-sejda:0.1.0")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.3")
    implementation("org.apache.commons:commons-collections4:4.5.0")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("xuggle:xuggle-xuggler-server-all:5.7.0-SNAPSHOT") {
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "ch.qos.logback", module = "logback-core")
    }
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
