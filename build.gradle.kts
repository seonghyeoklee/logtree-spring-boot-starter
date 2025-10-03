import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21" apply false
    kotlin("plugin.spring") version "1.9.21" apply false
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    `maven-publish`
    `java-library`
}

allprojects {
    group = "com.github.logtree"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                
                pom {
                    name.set("LogTree")
                    description.set("Hierarchical tracing library for Spring Boot applications")
                    url.set("https://github.com/yourusername/logtree")
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("yourid")
                            name.set("Your Name")
                            email.set("your@email.com")
                        }
                    }
                    
                    scm {
                        url.set("https://github.com/yourusername/logtree")
                        connection.set("scm:git:git://github.com/yourusername/logtree.git")
                        developerConnection.set("scm:git:ssh://github.com:yourusername/logtree.git")
                    }
                }
            }
        }
    }
}