plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":logtree-spring-boot-starter"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Spring Transaction Management (for @Transactional testing)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // H2 Database
    runtimeOnly("com.h2database:h2")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}