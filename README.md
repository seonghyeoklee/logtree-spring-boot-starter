# LogTree - Hierarchical Tracing Library for Spring Boot

LogTree is a lightweight, hierarchical tracing library designed for Spring Boot applications. It provides visual tree-structured logs that make it easy to track request flows, debug complex systems, and understand error causality chains.

## Features

- **Hierarchical Logging**: Visual tree structure for nested operations
- **Automatic Tracing**: AOP-based automatic method tracing with `@Traceable` annotation
- **Causality Chain**: Track error cause relationships through multiple layers
- **Spring Boot Integration**: Zero-configuration with Spring Boot auto-configuration
- **Performance Optimized**: Minimal overhead with optional async logging
- **Loki/Grafana Ready**: Structured logs optimized for Loki queries

## Quick Start

### 1. Add Dependency

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.yourusername:logtree:1.0.0")
}
```

> **Note**: Replace `yourusername` with your actual GitHub username

### 2. Basic Usage

```kotlin
@RestController
class UserController(private val userService: UserService) {
    
    @GetMapping("/users/{id}")
    @Traceable(name = "getUserById", includeArgs = true)
    fun getUser(@PathVariable id: Long): User {
        return userService.findById(id)
    }
}

@Service
class UserService {
    
    @Traceable(name = "findUserInDatabase")
    fun findById(id: Long): User {
        // Your business logic
    }
}
```

### 3. Output Example

```
14:32:15.123 [INFO ] [http-exec-1] [abc-123] ┌─ getUserById started
14:32:15.125 [INFO ] [http-exec-1] [abc-123] ├─ findUserInDatabase started
14:32:15.145 [INFO ] [http-exec-1] [abc-123] ├─ findUserInDatabase completed (20ms)
14:32:15.146 [INFO ] [http-exec-1] [abc-123] ┌─ getUserById completed (23ms)
```

## Advanced Features

### Manual Tracing

```kotlin
import io.github.logtree.core.LogTree

fun processOrder(order: Order) {
    LogTree.trace("process-order") {
        LogTree.span("validate-order") {
            validateOrder(order)
        }
        
        LogTree.span("calculate-pricing") {
            calculatePricing(order)
        }
        
        LogTree.span("save-to-database") {
            saveOrder(order)
        }
    }
}
```

### Causality Chain for Error Tracking

```kotlin
@Traceable(trackErrors = true)
fun riskyOperation() {
    try {
        performDatabaseOperation()
    } catch (e: Exception) {
        CausalityChain.current()
            .addCause("Database operation failed", "Connection timeout")
            .addException(e)
            .log()
        throw e
    }
}
```

Output:
```
Causality Chain:
┌─ Database operation failed
│  └─ caused by: Connection timeout
├─ IllegalStateException: Database access failed
│  └─ caused by: ConnectException: Connection refused
└─ ConnectException: Connection refused: Database is down
```

## Configuration

```yaml
# application.yml
logtree:
  enabled: true
  auto-trace-controllers: true
  auto-trace-services: false
  include-headers: false
  exclude-urls:
    - /health
    - /actuator/**
  max-depth: 10
  colored-output: true
  log-style: tree  # Options: tree, flat, json
```

## Annotation Options

```kotlin
@Traceable(
    name = "customSpanName",        # Custom span name (default: method name)
    includeArgs = true,              # Log method arguments
    includeResult = true,            # Log return value
    trackErrors = true,              # Track errors with causality chain
    tags = ["critical", "payment"]   # Add tags to span
)
```

## Logback Configuration

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%-5level] [%thread] [%X{traceId}] %X{visual} %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

## Loki/Grafana Integration

### Loki Query Examples

```promql
# Get all logs for a specific trace
{app="myapp"} | json | traceId="abc-123" | line_format "{{.visual}} {{.message}}"

# Find slow operations
{app="myapp"} | json | duration > 1000

# Error traces with causality
{app="myapp"} | json | level="ERROR" | line_format "{{.causality_chain}}"
```

## Building from Source

```bash
# Clone repository
git clone https://github.com/yourusername/logtree.git
cd logtree

# Build
./gradlew build

# Run sample application
./gradlew :logtree-sample:bootRun

# Test endpoints
curl http://localhost:8080/api/users/1
curl http://localhost:8080/api/users/error-demo
```

## Sample Application

The `logtree-sample` module contains a complete example application demonstrating:

- REST API with automatic tracing
- Service layer with nested spans
- Error handling with causality chains
- Custom trace configuration

## Performance Considerations

- Minimal overhead: ~0.1ms per span
- Async logging available for high-throughput applications
- Thread-local storage for context propagation
- Automatic cleanup to prevent memory leaks

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License

## Roadmap

- [ ] Support for reactive streams (WebFlux)
- [ ] Distributed tracing support (OpenTelemetry integration)
- [ ] Metrics collection and export
- [ ] Advanced sampling strategies
- [ ] Web UI for trace visualization