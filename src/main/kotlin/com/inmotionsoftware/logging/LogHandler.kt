/***************************************************************************
 * This source file is part of the kotlin-log open source project.         *
 *                                                                         *
 * Copyright (c) 2020-present, InMotion Software and the project authors   *
 * Licensed under the MIT License                                          *
 *                                                                         *
 * See LICENSE.txt for license information                                 *
 ***************************************************************************/

package com.inmotionsoftware.logging

import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 *  A `LogHandler` is an implementation of a logging backend.
 */
interface LogHandler {
    /**
     *  Emit a log message. There is no need for the `LogHandler` to check if the
     *  `level` is above or below the configured `logLevel` as `Logger` already performed this check and
     *  determined that a message should be logged.
     * 
     *  @param level: The log level the message was logged at.
     *  @param message: The message to log. To obtain a `String` representation call `message.description`.
     *  @param metadata: The metadata associated to this log message.
     *  @param source: The source where the log message originated, for example the logging module.
     *  @param file: The file the log message was emitted from.
     *  @param function: The function the log line was emitted from.
     *  @param line: The line the log message was emitted from.
     */
    fun log(level: LoggerLevel,
            message: LoggerMessage,
            metadata: LoggerMetadata?,
            source: String?,
            file: String?,
            function: String?,
            line: Int?)

    /**
     *  Get the logging metadata.
     *
     * @param metadataKey: The key for the metadata item
     * @return The metadata value for the given key. Null if not exist
     */
    operator fun get(metadataKey: String): LoggerMetadataValue?

    /**
     *  Set the logging metadata.
     *
     * @param metadataKey: The key for the metadata item
     * @param value: The metadata value to be set
     */
    operator fun set(metadataKey: String, value: LoggerMetadataValue)

    /** The entire metadata storage as a dictionary. */
    var metadata: LoggerMetadata

    /** The configured log level. */
    var logLevel: LoggerLevel
}


/**
 * A pseudo-`LogHandler` that can be used to send messages to multiple other `LogHandler`s.
 *
 * @param handlers: An array of `LogHandler`s, each of which will receive the log messages sent to this `Logger`.
 *                  The array must not be empty.
 */
class MultiplexLogHandler(private val handlers: List<LogHandler>) : LogHandler {
    private var effectiveLogLevel: LoggerLevel

    override var metadata: LoggerMetadata
        get() {
            val effectiveMetadata = LoggerMetadata(this.handlers.firstOrNull()?.metadata?.size ?: 0)
            return this.handlers.fold(effectiveMetadata) { acc, logHandler ->
                logHandler.metadata.entries.forEach {
                    acc.merge(it.key, it.value) { first, _ -> first }
                }
                acc
            }
        }
        set(value) {
            this.handlers.forEach { it.metadata = value }
        }

    override var logLevel: LoggerLevel
        get() = this.effectiveLogLevel
        set(value) {
            this.handlers.forEach { it.logLevel = value }
            this.effectiveLogLevel = value
        }

    init {
        if (this.handlers.isEmpty()) throw IllegalArgumentException("Handler list must not be empty")
        this.effectiveLogLevel = this.handlers.map { it.logLevel }.min() ?: LoggerLevel.Trace
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
        this.handlers.forEach {
            if (it.logLevel <= level) {
                it.log(level, message, metadata, source, file, function, line)
            }
        }
    }

    override operator fun get(metadataKey: String): LoggerMetadataValue? {
        for (handler in this.handlers) {
            return handler[metadataKey] ?: continue
        }
        return null
    }

    override operator fun set(metadataKey: String, value: LoggerMetadataValue) {
        this.handlers.forEach { it[metadataKey] = value }
    }

}

/**
 * `StreamLogHandler` is a simple implementation of `LogHandler` for directing
 * `Logger` output to System.out or System.err via the factory methods.
 */
class StreamLogHandler
    internal constructor(private val label: String, private val stream: OutputStream)
    : LogHandler {

    companion object{
        /** Factory that makes a `StreamLogHandler` to directs its output to System.out */
        fun standardOutput(label: String): StreamLogHandler {
            return StreamLogHandler(label, stream = System.out)
        }

        /** Factory that makes a `StreamLogHandler` to directs its output to System.err */
        fun standardError(label: String): StreamLogHandler {
            return StreamLogHandler(label, stream = System.err)
        }
    }

    override var logLevel: LoggerLevel = LoggerLevel.Info

    private var prettyMetadata: String? = null
    override var metadata: LoggerMetadata = LoggerMetadata()
        set(value) {
            field = value
            this.prettyMetadata = this.prettify(value)
        }

    private val dateTimeFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }

    override operator fun get(metadataKey: String): LoggerMetadataValue? {
        return this.metadata[metadataKey]
    }

    override operator fun set(metadataKey: String, value: LoggerMetadataValue) {
        this.metadata[metadataKey] = value
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
        val prettyMetadata =
                if (metadata.isNullOrEmpty())
                    this.prettyMetadata
                else {
                    metadata.entries.forEach {
                        this.metadata.merge(it.key, it.value) { first, _ -> first }
                    }
                    this.prettify(this.metadata)
                }

        val stream = this.stream
        try {
            stream.write("${this.timestamp()} $level ${this.label}:${prettyMetadata?.let {" $it"} ?: ""} $message\n".toByteArray(Charsets.UTF_8))
            stream.flush()
        } catch(e: Throwable) {
        }
    }

    private fun prettify(metadata: LoggerMetadata): String? {
        return if (metadata.isNotEmpty()) metadata.entries.joinToString(separator = " ") {"${it.key}=${it.value}"} else null
    }

    private fun timestamp(): String {
        return LocalDateTime.now().format(this.dateTimeFormatter)
    }

}

/**
 * No operation LogHandler, used when no logging is required.
 */
class KotlinLogNoOpLogHandler: LogHandler {
    override var metadata: LoggerMetadata
        get() { return emptyMap<String, LoggerMetadataValue>() as LoggerMetadata }
        set(_) { }

    override var logLevel: LoggerLevel
        get() { return LoggerLevel.Critical }
        set(_) {}

    override operator fun get(metadataKey: String): LoggerMetadataValue? { return  null }
    override operator fun set(metadataKey: String, value: LoggerMetadataValue) { }

    override fun log(
            level: LoggerLevel,
            message: LoggerMessage,
            metadata: LoggerMetadata?,
            source: String?,
            file: String?,
            function: String?,
            line: Int?
    ) { }
}
