plugins {
    kotlin("jvm") version "1.7.10"
    id("com.apollographql.apollo3") version "3.7.5"
    id("maven-publish")
}

val libName = "Kibber"
group = "no.iktdev.apis"
version = "1.2-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.apollographql.apollo3:apollo-runtime:3.7.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.springframework.graphql:spring-graphql-test:1.0.1")
}

apollo {
    service("service") {
        packageName.set("no.iktdev.tibber")
        schemaFile.set(file("src/main/graphql/no/iktdev/tibber/schema.graphqls"))
    }
    generateKotlinModels.set(true)
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
        dependsOn("generateVersionProperties")
    }
    test {
        useJUnitPlatform()
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
        }
        dependsOn("generateVersionProperties")
    }

    register("generateVersionProperties") {
        doFirst {
            val versionFile = File("${projectDir}/src/main/resources/version.properties")
            versionFile.parentFile.mkdirs()
            versionFile.writeText("version=${project.version}")
        }
    }
}

val reposiliteUrl = if (version.toString().endsWith("SNAPSHOT")) {
    "https://reposilite.iktdev.no/snapshots"
} else {
    "https://reposilite.iktdev.no/releases"
}

publishing {
    publications {
        create<MavenPublication>("reposilite") {
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set(libName)
                description.set("Streamit Subtitle Convert Library")
                version = project.version.toString()
                url.set(reposiliteUrl)
            }
            from(components["kotlin"])
        }
    }
    repositories {
        maven {
            name = libName
            url = uri(reposiliteUrl)
            credentials {
                username = System.getenv("reposiliteUsername")
                password = System.getenv("reposilitePassword")
            }
        }
    }
}