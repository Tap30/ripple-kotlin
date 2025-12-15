# Ripple Kotlin SDK - Onboarding Documentation

## Project Context & Knowledge Base

### Original Project Analysis
This project is a Kotlin implementation of the Ripple TypeScript SDK, based on the comprehensive analysis of `../ripple-ts` repository. The original TypeScript implementation provides a high-performance, scalable, and fault-tolerant event tracking SDK system.

### User Requirements & Vision
- **Primary Goal**: Create Kotlin duplication of ripple-ts repo
- **Target Platforms**: 
  - Android (Kotlin & Java)
  - Backend Spring (Kotlin & Java)
  - Reactive systems (Kotlin & Java)
- **Architecture**: Maximum shared code in core module with platform-specific adapters
- **Build System**: Gradle with Kotlin as primary language
- **Java Interoperability**: Full compatibility with Java systems (Android & backend)

### Core Features from TypeScript Analysis
- **Type-Safe Metadata Management**: Generic metadata types with full support
- **Unified Metadata System**: Merges shared metadata with event-specific metadata
- **Automatic Batching**: Configurable batch size with auto-flush
- **Scheduled Flushing**: Time-based automatic event dispatch
- **Retry Logic**: Exponential backoff with jitter (1000ms × 2^attempt + random jitter)
- **Event Persistence**: Automatic storage of unsent events
- **Queue Management**: Efficient FIFO queue using linked list (O(1) operations)
- **Graceful Degradation**: Re-queues events on failure after max retries
- **Custom Adapters**: Pluggable HTTP, storage, and logger implementations
- **Concurrency Safety**: Mutex-protected operations prevent race conditions

### Architecture Design Principles

#### 1. Modular Structure
```
ripple-kotlin/
├── core/                    # Shared core logic (Kotlin)
├── android/                 # Android-specific SDK
├── spring/                  # Spring Boot integration
├── reactive/                # Reactive streams support
└── samples/                 # Usage examples
```

#### 2. Adapter Pattern Implementation
- **HttpAdapter**: Abstract HTTP communication (OkHttp, Ktor, etc.)
- **StorageAdapter**: Abstract persistence (SharedPreferences, Room, File, Redis)
- **LoggerAdapter**: Abstract logging (Android Log, SLF4J, Logback)

#### 3. Type Safety & Generics
- Generic metadata types: `RippleClient<TEvents, TMetadata>`
- Event type mapping for compile-time safety
- Kotlin-specific features: data classes, sealed classes, coroutines

#### 4. Concurrency Model
- Kotlin Coroutines for async operations
- Mutex for thread-safe operations
- Flow for reactive streams
- Structured concurrency principles

### Platform-Specific Considerations

#### Android Platform
- **Storage**: SharedPreferences, Room Database, SQLite
- **HTTP**: OkHttp, Retrofit integration
- **Lifecycle**: Activity/Fragment lifecycle awareness
- **Threading**: Main thread safety, background processing
- **Permissions**: Network permissions handling

#### Spring Backend Platform
- **Storage**: JPA, Redis, File system
- **HTTP**: WebClient, RestTemplate
- **Configuration**: Spring Boot auto-configuration
- **Metrics**: Micrometer integration
- **Profiles**: Environment-specific configurations

#### Reactive Platform
- **Streams**: Kotlin Flow, RxJava compatibility
- **Backpressure**: Flow collection strategies
- **Error Handling**: Reactive error propagation
- **Integration**: Spring WebFlux, R2DBC

### Technical Implementation Strategy

#### Core Module Design
```kotlin
// Core interfaces and implementations
interface RippleClient<TEvents, TMetadata>
class MetadataManager<TMetadata>
class Dispatcher<TMetadata>
class Queue<T>
class Mutex

// Adapter interfaces
interface HttpAdapter
interface StorageAdapter
interface LoggerAdapter
```

#### Platform Adapters
```kotlin
// Android
class OkHttpAdapter : HttpAdapter
class SharedPreferencesAdapter : StorageAdapter
class AndroidLogAdapter : LoggerAdapter

// Spring
class WebClientAdapter : HttpAdapter
class JpaStorageAdapter : StorageAdapter
class Slf4jLoggerAdapter : LoggerAdapter

// Reactive
class ReactiveHttpAdapter : HttpAdapter
class ReactiveStorageAdapter : StorageAdapter
```

### Development Guidelines

#### Kotlin Best Practices
- Use data classes for immutable data structures
- Leverage sealed classes for type-safe state management
- Implement suspend functions for async operations
- Use inline classes for type-safe wrappers
- Apply scope functions appropriately

#### Java Interoperability
- Use `@JvmStatic` for static methods
- Apply `@JvmOverloads` for default parameters
- Use `@JvmName` for method name conflicts
- Avoid Kotlin-specific features in public APIs when Java compatibility is required

#### Testing Strategy
- Unit tests for core logic (JUnit 5, MockK)
- Integration tests for platform adapters
- Android instrumented tests for Android module
- Spring Boot tests for Spring module
- 100% code coverage requirement

#### Build Configuration
- Multi-module Gradle project
- Kotlin Multiplatform considerations
- Version catalogs for dependency management
- Gradle convention plugins for shared configuration

### API Contract Alignment

The Kotlin implementation follows the same API contract as the TypeScript version:

```kotlin
// Configuration
data class RippleConfig(
    val apiKey: String,
    val endpoint: String,
    val apiKeyHeader: String = "X-API-Key",
    val flushInterval: Long = 5000L,
    val maxBatchSize: Int = 10,
    val maxRetries: Int = 3,
    val adapters: AdapterConfig
)

// Client interface
interface RippleClient<TEvents, TMetadata> {
    suspend fun init()
    suspend fun track(name: String, payload: Map<String, Any>? = null, metadata: TMetadata? = null)
    fun setMetadata(key: String, value: Any)
    suspend fun flush()
    fun dispose()
}
```

### Migration Considerations

#### From TypeScript to Kotlin
- **Promises → Coroutines**: Async operations using suspend functions
- **Interfaces → Interfaces**: Direct translation with Kotlin syntax
- **Generics → Generics**: Enhanced with Kotlin's type system
- **Error Handling**: Kotlin's Result type and exception handling
- **Collections**: Kotlin's immutable collections by default

#### Platform Migration Paths
- **Browser → Android**: Similar lifecycle and storage concepts
- **Node.js → Spring**: Server-side event tracking patterns
- **File Storage → Database**: Enhanced persistence options

### Quality Assurance

#### Code Quality
- Detekt for static analysis
- KtLint for code formatting
- Gradle dependency analysis
- API compatibility checks

#### Performance
- Coroutine performance profiling
- Memory usage optimization
- Battery usage considerations (Android)
- Throughput benchmarks

#### Security
- API key protection
- Network security (certificate pinning)
- Data encryption at rest
- Privacy compliance (GDPR, CCPA)

### Documentation Strategy

#### Developer Documentation
- KDoc for all public APIs
- Usage examples for each platform
- Migration guides from other SDKs
- Best practices documentation

#### Integration Guides
- Android integration with popular libraries
- Spring Boot starter configuration
- Reactive streams integration patterns
- Testing strategies and mocking

### Future Considerations

#### Kotlin Multiplatform
- Potential expansion to iOS, Desktop, Web
- Shared business logic across all platforms
- Platform-specific UI integrations

#### Performance Optimizations
- Native compilation considerations
- Memory-mapped file storage
- Batch compression algorithms
- Network optimization strategies

---

## AI Assistant Context

This document serves as the comprehensive knowledge base for AI assistants working on this project. It contains:

1. **Project Requirements**: User's vision and technical requirements
2. **Architecture Decisions**: Design principles and implementation strategy  
3. **Platform Considerations**: Specific needs for Android, Spring, and Reactive platforms
4. **Technical Guidelines**: Kotlin best practices and Java interoperability
5. **Quality Standards**: Testing, documentation, and performance requirements

When working on this project, refer to this document for context and ensure all implementations align with the established principles and requirements.
