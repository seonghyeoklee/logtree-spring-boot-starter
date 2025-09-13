plugins {
    kotlin("jvm")
}

dependencies {
    // Logging
    api("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // Optional Spring Support (for AOP)
    compileOnly("org.springframework:spring-context:6.1.2")
    compileOnly("org.aspectj:aspectjweaver:1.9.20.1")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "LogTree Core",
            "Implementation-Version" to project.version
        )
    }
}