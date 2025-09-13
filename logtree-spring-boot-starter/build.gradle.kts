plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    api(project(":logtree-core"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-aop:3.2.0")
    implementation("org.springframework.boot:spring-boot-autoconfigure:3.2.0")
    
    // Annotation processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.2.0")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "LogTree Spring Boot Starter",
            "Implementation-Version" to project.version
        )
    }
}