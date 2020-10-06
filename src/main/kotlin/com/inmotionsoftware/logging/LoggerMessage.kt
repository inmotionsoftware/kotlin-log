/***************************************************************************
 * This source file is part of the kotlin-log open source project.         *
 *                                                                         *
 * Copyright (c) 2020-present, InMotion Software and the project authors   *
 * Licensed under the MIT License                                          *
 *                                                                         *
 * See LICENSE.txt for license information                                 *
 ***************************************************************************/

package com.inmotionsoftware.logging

import java.io.PrintWriter
import java.io.StringWriter

/**
 *  `LoggerMessage` represents a log message's text. It is usually created using string literals.
 */
data class LoggerMessage(private val value: String): Comparable<LoggerMessage>, CharSequence {

    override fun toString(): String {
        return this.value
    }

    override val length: Int
        get() {
            return this.value.length
        }

    override fun equals(other: Any?): Boolean {
        val otherMessage = other as? LoggerMessage ?: return false
        return this.value == otherMessage.value
    }

    override fun get(index: Int): Char
            = this.value[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence
            = this.value.subSequence(startIndex, endIndex)

    override fun compareTo(other: LoggerMessage): Int {
        return this.value.compareTo(other.value)
    }
}

/**
 * String extension to return the string as LoggerMessage
 */
fun String.asLoggerMessage(): LoggerMessage {
    return LoggerMessage(this)
}

/**
 * Throwable extension to return the stack trace as LoggerMessage
 */
fun Throwable.asLoggerMessage(): LoggerMessage {
    return LoggerMessage(this.stackTraceString())
}

/**
 * Throwable extension to return the stack trace as String
 */
fun Throwable.stackTraceString(): String {
    val sw = StringWriter(256)
    val pw = PrintWriter(sw, false)
    this.printStackTrace(pw)
    pw.flush()
    return sw.toString()
}
