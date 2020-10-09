/***************************************************************************
 * This source file is part of the kotlin-log open source project.         *
 *                                                                         *
 * Copyright (c) 2020-present, InMotion Software and the project authors   *
 * Licensed under the MIT License                                          *
 *                                                                         *
 * See LICENSE.txt for license information                                 *
 ***************************************************************************/

package com.inmotionsoftware.logging

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.junit.Assert.*
import java.util.concurrent.Executors
import kotlin.math.max

///
/// TestLogging
///
internal class TestLogging {
    // shared among loggers
    private val _config = Config()
    // shared among loggers
    private val recorder = Recorder()

    val config: Config
        get() { return this._config }
    val history: History
        get() { return this.recorder }

    fun make(label: String): LogHandler {
        return TestLogHandler(label, this.config, this.recorder)
    }
}

///
/// TestLogHandler
///
internal class TestLogHandler(private val label: String, private val config: Config, private val recorder: Recorder) : LogHandler {
    private val logLevelLock = ReentrantLock()
    private val metadataLock = ReentrantLock()
    private val logger: Logger =
            Logger(label="test", handler=StreamLogHandler.standardOutput(label))  // The actual logger

    private var _logLevel: LoggerLevel? = null
    override var logLevel: LoggerLevel
        get() {
            return this.logLevelLock.withLock { this._logLevel } ?: this.config.getLoggerLevel(this.label)
        }
        set(value) {
            this.logLevelLock.withLock { this._logLevel = value }
        }

    private var _metadataSet = false
    private var _metadata: LoggerMetadata = LoggerMetadata()

    override var metadata: LoggerMetadata
        get() {
            return this.metadataLock.withLock { this._metadata }
        }
        set(value) {
            this.metadataLock.withLock { this._metadata = value }
        }

    init {
        this.logger.logLevel = LoggerLevel.Debug
    }

    override fun log(
        level: LoggerLevel,
        message: LoggerMessage,
        metadata: LoggerMetadata?,
        source: String?,
        file: String?,
        function: String?,
        line: Int?
    ) {
        val meta = if (this._metadataSet) HashMap(this.metadata) else HashMap(MDC.global.metadata)
        (metadata ?: LoggerMetadata()).forEach {
            meta.merge(it.key, it.value) { first, _ -> first }
        }

        this.logger.log(level, {message}, {meta}, {source}, file, function, line)
        this.recorder.record(level, meta, message, source ?: "")
    }

    override fun get(metadataKey: String): LoggerMetadataValue? {
        return this.metadataLock.withLock { this._metadata[metadataKey] }
    }

    override fun set(metadataKey: String, value: LoggerMetadataValue) {
        this.metadataLock.withLock {
            this._metadataSet = true
            this._metadata[metadataKey] = value
        }
    }

}

///
/// LogEntry
///
internal data class LogEntry(
    val level: LoggerLevel,
    val metadata: LoggerMetadata?,
    val message: String,
    val source: String
)

///
/// History
///
internal interface History {
    val entries: List<LogEntry>
}

internal fun History.atLevel(level: LoggerLevel): List<LogEntry> {
    return this.entries.filter { level == it.level }
}

internal val History.trace: List<LogEntry>
    get() { return this.atLevel(LoggerLevel.Trace) }

internal val History.debug: List<LogEntry>
    get() { return this.atLevel(LoggerLevel.Debug) }

internal val History.info: List<LogEntry>
    get() {  return this.atLevel(LoggerLevel.Info) }

internal val History.warning: List<LogEntry>
    get() {  return this.atLevel(LoggerLevel.Warning) }

internal val History.error: List<LogEntry>
    get() { return this.atLevel(LoggerLevel.Error) }

internal fun History.assertExist(
    level: LoggerLevel,
    message: String,
    metadata: LoggerMetadata? = null,
    location: LogLocation? = null
) {
    val source = location?.source ?: ""
    val entry = this.find(level, message, metadata, source)
    assertNotNull("Entry not found: $level ${location?.source} $metadata $message ${location?.file} ${location?.line}", entry)
}

internal fun History.assertNotExist(
    level: LoggerLevel,
    message: String,
    metadata: LoggerMetadata? = null,
    location: LogLocation? = null
) {
    val source = location?.source ?: ""
    val entry = this.find(level, message, metadata, source)
    assertNull("Entry was found: $level ${location?.source} $metadata $message ${location?.file} ${location?.line}", entry)
}

internal fun History.find(
    level: LoggerLevel,
    message: String,
    metadata: LoggerMetadata? = null,
    source: String
): LogEntry? {
    return this.entries.firstOrNull{ entry ->
        entry.level == level
        && entry.message == message
        && (entry.metadata ?: LoggerMetadata()) == (metadata ?: LoggerMetadata())
        && entry.source == source
    }
}

///
/// Recorder
///
internal class Recorder : History {
    private val lock = ReentrantLock()
    private var _entries = ArrayList<LogEntry>()

    override val entries: List<LogEntry>
        get() { return this.lock.withLock { this._entries } }

    fun record(
        level: LoggerLevel,
        metadata: LoggerMetadata?,
        message: LoggerMessage,
        source: String
    ) {
        this.lock.withLock {
            this._entries.add(LogEntry(level, metadata, message.toString(), source))
        }
    }
}

///
/// Config
///
internal class Config {
    companion object {
        const val ALL = "*"
    }

    private val lock = ReentrantLock()
    private var storage = HashMap<String, LoggerLevel>()

    fun getLoggerLevel(key: String): LoggerLevel {
        return this[key] ?: this[Config.ALL] ?: LoggerLevel.Debug
    }

    operator fun get(key: String): LoggerLevel? {
        return this.lock.withLock { this.storage[key] }
    }

    operator fun set(key: String = Config.ALL, value: LoggerLevel) {
        this.lock.withLock { this.storage[key] = value }
    }

    fun clear() {
        this.lock.withLock { this.storage.clear() }
    }
}

///
/// MDC
///
internal class MDC private constructor() {
    companion object {
        val global = MDC()
    }

    private val lock = ReentrantLock()
    private var storage = HashMap<Long, LoggerMetadata>()
    private val threadId: Long
        get() { return Thread.currentThread().id }

    val metadata: LoggerMetadata
        get() {
            return this.lock.withLock { this.storage[this.threadId] ?: LoggerMetadata() }
        }

    operator fun get(metadataKey: String): LoggerMetadataValue? {
        return this.lock.withLock { this.storage[this.threadId]?.get(metadataKey) }
    }

    operator fun set(metadataKey: String, value: LoggerMetadataValue?) {
        this.lock.withLock {
            val threadId = this.threadId
            if (this.storage[threadId] == null) {
                this.storage[threadId] = LoggerMetadata()
            }
            if (value == null) {
                this.storage[threadId]!!.remove(metadataKey)
            } else {
                this.storage[threadId]!![metadataKey] = value
            }
        }
    }

    fun clear() {
        this.lock.withLock { this.storage.remove(this.threadId) }
    }

    @Throws
    fun with(metadata: LoggerMetadata, body: () -> Unit) {
        metadata.forEach { (key, value) -> this[key] = value }
        try {
            return body()
        }
        finally {
            metadata.keys.forEach { this[it] = null }
        }
    }

    @Throws
    fun <T> with(metadata: LoggerMetadata, body: () -> T): T {
        metadata.forEach { (key, value) -> this[key] = value }
        try {
            return body()
        }
        finally {
            metadata.keys.forEach { this[it] = null }
        }
    }

    // For testing
    fun flush() {
        this.lock.withLock { this.storage.clear() }
    }

}

///
/// TestLibrary
///
internal class TestLibrary {
    private val logger = Logger("TestLibrary")
    private val executor = Executors.newCachedThreadPool()

    fun doSomething() {
        this.logger.info("TestLibrary::doSomething")
    }

    fun doSomethingAsync(completion: () -> Unit) {
        // libraries that use global loggers and async, need to make sure they propagate the
        // logging metadata when creating a new thread
        val metadata = HashMap(MDC.global.metadata)
        this.executor.execute {
            Thread.sleep((max(0.1, 0.0) * 1000).toLong())
            MDC.global.with(metadata) {
                this.logger.info("TestLibrary::doSomethingAsync")
                completion()
            }
        }
    }

}
