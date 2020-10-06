/***************************************************************************
 * This source file is part of the kotlin-log open source project.         *
 *                                                                         *
 * Copyright (c) 2020-present, InMotion Software and the project authors   *
 * Licensed under the MIT License                                          *
 *                                                                         *
 * See LICENSE.txt for license information                                 *
 ***************************************************************************/

package com.inmotionsoftware.logging

import java.lang.Exception

/**
 * LogLocation provides source information at the log line
 */
data class LogLocation(
    val source: String?,
    val file: String,
    val function: String,
    val line: Int
)

/**
 * Helper method for capturing file, function, and line information at the log location
 */
fun __location(error: Throwable? = null, source: String? = null): LogLocation {
    val e = error ?: Exception()
    val el = e.stackTrace[2]
    return LogLocation(source, el.fileName ?: "", el.methodName, el.lineNumber)
}

/**
 *  A `Logger` is the central type in `kotlin-log`. Its central function is to emit
 *  log messages using one of the methods corresponding to a log level.
 *
 *  @param label: An identifier of the creator of this `Logger`.
 *  @param handler: A LogHandler for emitting log messages.
 */
class Logger internal constructor(private var label: String, var handler: LogHandler) {

    constructor(label: String)
            : this(label, LoggingSystem.lock.withReaderLock { LoggingSystem.factory(label) })

    constructor(label: String, factory: (label: String) -> LogHandler)
            : this(label, factory(label))

    /* The log level configured for this `Logger`. */
    var Logger.logLevel: LoggerLevel
        get() = this.handler.logLevel
        set(newValue) {
            this.handler.logLevel = newValue
        }

    /**
     *  Get the logging metadata.
     *
     * @param metadataKey: The key for the metadata item
     * @return The metadata value for the given key. Null if not exist
     */
    operator fun get(metadataKey: String): LoggerMetadataValue? {
        return this.handler[metadataKey]
    }

    /**
     *  Set the logging metadata.
     *
     * @param metadataKey: The key for the metadata item
     * @param value: The metadata value to be set
     */
    operator fun set(metadataKey: String, value: LoggerMetadataValue) {
        this.handler[metadataKey] = value
    }

    /**
     *  Log a message passing the log level as a parameter.
     * 
     *  If the `logLevel` passed to this method is more severe than the `Logger`'s `logLevel`, it will be logged,
     *  otherwise nothing will happen.
     * 
     *  @param level: The log level to log `message` at. For the available log levels, see `LoggerLevel`.
     *  @param message: The message to be logged. `message` can be used with any string interpolation literal.
     *  @param metadata: One-off metadata to attach to this log message.
     *  @param source: The source this log messages originates to.
     *  @param file: The file this log message originates from.
     *  @param function: The function this log message originates from.
     *  @param line: The line this log message originates from.
     */
    inline
    fun log(
        level: LoggerLevel,
        message: () -> LoggerMessage,
        metadata: () -> LoggerMetadata? = { null },
        source: () -> String? = { null },
        file: String?,
        function: String?,
        line: Int?
    ) {
        if (level <= this.logLevel) {
            this.handler.log(
                level = level,
                message = message(),
                metadata = metadata(),
                source = source(),
                file = file,
                function = function,
                line = line
            )
        }
    }
}

/**
 *  Log a message passing with the `LoggerLevel.Trace` log level.
 * 
 *  If `LoggerLevel.Trace` is at least as severe as the `Logger`'s `logLevel`, it will be logged,
 *  otherwise nothing will happen.
 */
inline
fun Logger.trace(
    message: String,
    metadata: () -> LoggerMetadata? = { null },
    location: LogLocation? = null
) {
    this.log(
        LoggerLevel.Trace,
        {message.asLoggerMessage()},
        metadata,
        {location?.source},
        location?.file,
        location?.function,
        location?.line
    )
}

/**
 *  Log a message passing with the `LoggerLevel.Debug` log level.
 * 
 *  If `LoggerLevel.Debug` is at least as severe as the `Logger`'s `logLevel`, it will be logged,
 *  otherwise nothing will happen.
 */
inline
fun Logger.debug(
    message: String,
    metadata: () -> LoggerMetadata? = { null },
    location: LogLocation? = null
)
{
    this.log(
        LoggerLevel.Debug,
        {message.asLoggerMessage()},
        metadata,
        {location?.source},
        location?.file,
        location?.function,
        location?.line
    )
}

/**
 *  Log a message passing with the `LoggerLevel.Info` log level.
 * 
 *  If `LoggerLevel.Info` is at least as severe as the `Logger`'s `logLevel`, it will be logged,
 *  otherwise nothing will happen.
 */
inline
fun Logger.info(
    message: String,
    metadata: () -> LoggerMetadata? = { null },
    location: LogLocation? = null
) {
    this.log(
        LoggerLevel.Info,
        {message.asLoggerMessage()},
        metadata,
        {location?.source},
        location?.file,
        location?.function,
        location?.line
    )
}

/**
 *  Log a message passing with the `LoggerLevel.Notice` log level.
 * 
 *  If `LoggerLevel.Notice` is at least as severe as the `Logger`'s `logLevel`, it will be logged,
 *  otherwise nothing will happen.
 */
inline
fun Logger.notice(
    message: String,
    metadata: () -> LoggerMetadata? = { null },
    location: LogLocation? = null
) {
    this.log(
        LoggerLevel.Notice,
        {message.asLoggerMessage()},
        metadata,
        {location?.source},
        location?.file,
        location?.function,
        location?.line
    )
}

/*
 *  Log a message passing with the `LoggerLevel.Warning` log level.
 * 
 *  If `LoggerLevel.Warning` is at least as severe as the `Logger`'s `logLevel`, it will be logged,
 *  otherwise nothing will happen.
 */
inline
fun Logger.warning(
    message: String,
    metadata: () -> LoggerMetadata? = { null },
    location: LogLocation? = null
) {
    this.log(
        LoggerLevel.Warning,
        {message.asLoggerMessage()},
        metadata,
        {location?.source},
        location?.file,
        location?.function,
        location?.line
    )
}

/*
 *  Log a message passing with the `LoggerLevel.Error` log level.
 * 
 *  If `LoggerLevel.Error` is at least as severe as the `Logger`'s `logLevel`, it will be logged,
 *  otherwise nothing will happen.
 */
inline
fun Logger.error(
        message: String,
        metadata: () -> LoggerMetadata? = { null },
        location: LogLocation? = null
) {
    this.log(
        LoggerLevel.Error,
        {message.asLoggerMessage()},
        metadata,
        {location?.source},
        location?.file,
        location?.function,
        location?.line
    )
}

/*
 *  Log a message passing with the `LoggerLevel.Error` log level.
 *
 *  If `LoggerLevel.Error` is at least as severe as the `Logger`'s `logLevel`, it will be logged,
 *  otherwise nothing will happen.
 */
inline
fun Logger.error(
        error: Throwable,
        metadata: () -> LoggerMetadata? = { null },
        location: LogLocation? = null
) {
    this.log(
            LoggerLevel.Error,
            {error.asLoggerMessage()},
            metadata,
            {location?.source},
            location?.file,
            location?.function,
            location?.line
    )
}

/*
 *  Log a message passing with the `LoggerLevel.Critical` log level.
 * 
 *  `LoggerLevel.Critical` messages will always be logged.
 */
inline
fun Logger.critical(
    message: String,
    metadata: () -> LoggerMetadata? = { null },
    location: LogLocation? = null
) {
    this.log(
        LoggerLevel.Critical,
        {message.asLoggerMessage()},
        metadata,
        {location?.source},
        location?.file,
        location?.function,
        location?.line
    )
}

/*
 *  Log a message passing with the `LoggerLevel.Critical` log level.
 *
 *  `LoggerLevel.Critical` messages will always be logged.
 */
inline
fun Logger.critical(
        error: Throwable,
        metadata: () -> LoggerMetadata? = { null },
        location: LogLocation? = null
) {
    this.log(
            LoggerLevel.Critical,
            {error.asLoggerMessage()},
            metadata,
            {location?.source},
            location?.file,
            location?.function,
            location?.line
    )
}
