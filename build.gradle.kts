plugins {
    id("java-library")
    id("maven-publish")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("signing")
}


repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    api("com.amazonaws:dynamodb-lock-client:1.2.0")
    implementation("software.momento.java:sdk:1.4.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("commons-logging:commons-logging:1.2")
    implementation("software.amazon.awssdk:aws-core:2.20.8")
    implementation("software.amazon.awssdk:dynamodb:2.20.8")
    implementation("software.amazon.awssdk:sdk-core:2.20.8")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.9.3")
        }
    }
}

group = "software.momento.java"

// Use a default SNAPSHOT version if the environment variable cannot be found.
// The version is specified here to prevent an inconsistent version from being seen by different tasks.
version = System.getenv("MOMENTO_DDB_LOCK_VERSION") ?: "v0.1.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
    // Produce an artifact build for Java 8+
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

// Only run publishing tasks if the required environment variables are present
val safeToPublish = provider {
    !System.getenv("MOMENTO_DDB_LOCK_VERSION").isNullOrEmpty() &&
            !System.getenv("SONATYPE_USERNAME").isNullOrEmpty() &&
            !System.getenv("SONATYPE_PASSWORD").isNullOrEmpty() &&
            !System.getenv("SONATYPE_SIGNING_KEY").isNullOrEmpty() &&
            !System.getenv("SONATYPE_SIGNING_KEY_PASSWORD").isNullOrEmpty()
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    onlyIf {
        safeToPublish.get()
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = rootProject.name

            pom {
                name.set("Momento DynamoDB Lock Client")
                description.set("A Momento backed drop-in replacement for Amazon DynamoDB Lock Client")
                url.set("https://github.com/momentohq/momento-dynamodb-lock-client")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("momento")
                        name.set("Momento")
                        organization.set("Momento")
                        email.set("eng-deveco@momentohq.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/momentohq/momento-dynamodb-lock-client.git")
                    developerConnection.set("scm:git:git@github.com:momentohq/momento-dynamodb-lock-client.git")
                    url.set("https://github.com/momentohq/momento-dynamodb-lock-client")
                }
            }
        }
    }
}

tasks.withType<io.github.gradlenexus.publishplugin.AbstractNexusStagingRepositoryTask>().configureEach {
    onlyIf {
        safeToPublish.get()
    }
}
nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("SONATYPE_USERNAME"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf {
        safeToPublish.get()
    }
}

signing {
    val signingKey = System.getenv("SONATYPE_SIGNING_KEY")
    val signingPassword = System.getenv("SONATYPE_SIGNING_KEY_PASSWORD")

    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["mavenJava"])
}
