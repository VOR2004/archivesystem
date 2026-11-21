plugins {
    id("java")
}

group = "ru.itis"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

val gsonVersion: String by project

dependencies {
    implementation("com.google.code.gson:gson:$gsonVersion")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}

// Optional local package. Terraform uploads sources for an automatic cloud-side Maven build.
tasks.register<Zip>("functionZip") {
    dependsOn(tasks.jar)
    archiveFileName.set("function.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
