
# Kotlin port of SwiftLog
#### https://github.com/apple/swift-log

## KotlinLog
A Logging API package for Kotlin. Version `1.4.0` is the first release version and is a port of SwiftLog version `1.4.0`.

The primary goal of this project is to promote code portability between Swift and Kotlin. It is useful for organizations want to implement native mobile apps but have not found or cannot adopt a cross-platform mobile framework. By using common libraries with exact API, business logics written in Swift or Kotlin can be easily ported to the other platform.  

[PromiseKt](https://github.com/inmotionsoftware/promisekt) is another example of this effort.
 
Similar to `SwiftLog`, `KotlinLog` is an _API Package_ which tries to establish a common API the ecosystem can use. To make logging really work for real-world workloads, we need `KotlinLog`-compatible _logging backends_ which then either persist the log messages in files, write to Logcat, or send them over to cloud services.

## Getting started
If you have a JVM application written in Kotlin, or an Android app, and would like to log, using this API package can get you started quickly.

#### Adding the dependency

```
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}
dependencies {
    implementation "com.github.inmotionsoftware:kotlin-log:1.4.0"
}
```

#### Let's log

```kotlin
// 1) let's import the logging API package
import com.inmotionsoftware.logging.Logger

// 2) we need to create a logger, the label works similarly to a DispatchQueue label
val logger = Logger(label= "com.example.BestExampleApp.main")

// 3) we're now ready to use it
logger.info("Hello World!")
```
 
#### Output

```
2020-10-09 14:55:51.611 Info com.example.BestExampleApp.main: Hello World!
```
#### Log with location
```kotlin
// log with file, method, and line information
logger.debug("Should not occur here", location=__location())

// log with throwable
try {
    ...    
} catch (e: Throwable) {
    logger.error(e, location=__location(error=e))
}
```
#### Default `Logger` behavior

`KotlintLog` provides for very basic console logging out-of-the-box by way of `StreamLogHandler`. It is possible to switch the default output to `System.err` like so:
```kotlin
LoggingSystem.bootstrap { StreamLogHandler.standardError(it) }
```

`StreamLogHandler` is primarily a convenience only and does not provide any substantial customization. Library maintainers who aim to build their own logging backends for integration and consumption should implement the `LogHandler` protocol directly.

#### Selecting a logging backend implementation (applications only)

As the API has just launched, not many implementations exist yet. If you are interested in implementing one see the "Implementation considerations" section below explaining how to do so. List of existing SwiftLog API compatible libraries:

| Repository | Handler Description|
| ----------- | ----------- |
| [inmotionsoftware/kotlin-log-android](https://github.com/inmotionsoftware/kotlin-log-android)  |A logging backend for `KotlinLog` that sends log messages to `Logcat` |
| [inmotionsoftware/kotlin-log-analytics-firebase](https://github.com/inmotionsoftware/kotlin-log-analytics-firebase)  |A logging backend for `KotlinLog` that sends analytics log messages to `Firebase`|

## What is an API package?
Please see `SwiftLog`'s [What is an API package](https://github.com/apple/swift-log/blob/main/README.md#what-is-an-api-package) section.

## The core concepts

### Loggers

`Logger`s are used to emit log messages and therefore the most important type in `KotlinLog`, so their use should be as simple as possible.  Most commonly, they are used to emit log messages in a certain log level. For example:

```kotlin
// logging an informational message
logger.info("Hello World!")

// ouch, something went wrong
logger.error("Houston, we have a problem: $problem")
```

### Log levels

The following log levels are supported:

 - `Trace`
 - `Debug`
 - `Info`
 - `Notice`
 - `Warning`
 - `Error`
 - `Critical`

The log level of a given logger can be changed, but the change will only affect the specific logger you changed it on.

### Logging metadata

Logging metadata is metadata that can be attached to loggers to add information that is crucial when debugging a problem. In servers, the usual example is attaching a request UUID to a logger that will then be present on all log messages logged with that logger. Example:

```kotlin
val logger = Logger(label="Test logger")
logger["request-uuid"] = "${UUID.randomUUID()}".asLoggerMetadataValue()
logger.info("hello world")
```

will print

```
2020-10-09 15:11:56.810 Info Test logger: request-uuid=String(value=e8733b5d-e5d9-4b74-9872-8f850edc7711) hello world
```

with the default logging backend implementation that ships with `KotlinLog`. Needless to say, the format is fully defined by the logging backend you choose.

## On the implementation of a logging backend (a `LogHandler`)

Note: If you don't want to implement a custom logging backend, everything in this section is probably not very relevant, so please feel free to skip.

To become a compatible logging backend that all `KotlinLog` consumers can use, you need to do two things: 1) Implement a class implements `LogHandler`, an interface provided by `KotlinLog` and 2) instruct `KotlinLog` to use your logging backend implementation.

A `LogHandler` or logging backend implementation is anything that conforms to the following interface

```kotlin
interface LogHandler {
    fun log(level: LoggerLevel,
            message: LoggerMessage,
            metadata: LoggerMetadata?,
            source: String?,
            file: String?,
            function: String?,
            line: Int?)

    operator fun get(metadataKey: String): LoggerMetadataValue?

    operator fun set(metadataKey: String, value: LoggerMetadataValue)

    var metadata: LoggerMetadata

    var logLevel: LoggerLevel
}
```

Instructing `KotlinLog` to use your logging backend as the one the whole application (including all libraries) should use is very simple:

```kotlin
LoggingSystem.bootstrap { MyLogHandler() }
```

### Implementation considerations

`LogHandler`s control most parts of the logging system:

#### Under control of a `LogHandler`

##### Configuration

`LogHandler`s control the two crucial pieces of `Logger` configuration, namely:

- log level (`logger.logLevel` property)
- logging metadata (`logger[metadataKey:]` and `logger.metadata`)

For the system to work, however, it is important that `LogHandler` treat the configuration as _value types_. This means that `LogHandler`s should NOT be shared or create a new instance as needed.

However, in special cases, it is acceptable that a `LogHandler` provides some global log level override that may affect all `LogHandler`s created.

##### Emitting
- emitting the log message itself

### Not under control of `LogHandler`s

`LogHandler`s do not control if a message should be logged or not. `Logger` will only invoke the `log` function of a `LogHandler` if `Logger` determines that a log message should be emitted given the configured log level.

## Source vs Label

A `Logger` carries a `label` and each log message may carry a `source` parameter. The `Logger`'s label
identifies the creator of the `Logger`. If you are using structured logging by preserving metadata across multiple modules, the `Logger`'s
`label` is not a good way to identify where a log message originated from as it identifies the creator of a `Logger` which is often passed
around between libraries to preserve metadata and the like.

If you want to filter all log messages originating from a certain subsystem, filter by `source` which defaults to the module that is emitting the
log message.

## License

MIT