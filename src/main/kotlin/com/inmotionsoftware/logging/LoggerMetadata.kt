/***************************************************************************
 * This source file is part of the kotlin-log open source project.         *
 *                                                                         *
 * Copyright (c) 2020-present, InMotion Software and the project authors   *
 * Licensed under the MIT License                                          *
 *                                                                         *
 * See LICENSE.txt for license information                                 *
 ***************************************************************************/

package com.inmotionsoftware.logging

/**
 * `Metadata` is a typealias for `MutableMap<String, LoggerMetadataValue>`
 * the type of the metadata storage.
 */
typealias LoggerMetadata = MutableMap<String, LoggerMetadataValue>

/**
 * Helper method for creating a LoggerMetadata for the given initialCapacity.
 */
fun LoggerMetadata(initialCapacity: Int = 10): LoggerMetadata {
    return HashMap(initialCapacity)
}

/**
 * A logging metadata value. `LoggerMetadataValue` is string, array, and dictionary.
 */
sealed class LoggerMetadataValue {
    data class String(val value: kotlin.String): LoggerMetadataValue()
    data class Dictionary(val value: LoggerMetadata): LoggerMetadataValue()
    data class Array(val value: List<LoggerMetadataValue>): LoggerMetadataValue()
}

/**
 * String extension to return the string as LoggerMetadataValue
 */
fun String.asLoggerMetadataValue(): LoggerMetadataValue {
    return LoggerMetadataValue.String(this)
}
