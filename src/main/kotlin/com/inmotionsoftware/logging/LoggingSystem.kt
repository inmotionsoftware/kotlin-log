/***************************************************************************
 * This source file is part of the kotlin-log open source project.         *
 *                                                                         *
 * Copyright (c) 2020-present, InMotion Software and the project authors   *
 * Licensed under the MIT License                                          *
 *                                                                         *
 * See LICENSE.txt for license information                                 *
 ***************************************************************************/

package com.inmotionsoftware.logging

import java.lang.IllegalStateException
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * The `LoggingSystem` is a global facility where the default logging backend implementation (`LogHandler`) can be
 * configured. `LoggingSystem` is set up just once in a given program to set up the desired logging backend implementation.
 */
class LoggingSystem  {
    companion object  {
        internal val lock = ReentrantReadWriteLock()
        internal var factory: (label: String) -> LogHandler = { StreamLogHandler.standardError(it) }
        private var initialized = false

        fun bootstrap(factory: (label: String) -> LogHandler) {
            this.lock.withWriterLock {
                if (this.initialized) throw IllegalStateException("Logging system can only be initialized once per process.")
                this.factory = factory
                this.initialized = true
            }
        }
    }
}

/**
 * ReadWriteLock extension to execute the given code block in the write lock.
 */
fun <T> ReadWriteLock.withWriterLock(block: () -> T): T {
    return this.writeLock().withLock(block)
}

/**
 * ReadWriteLock extension to execute the given code block in the read lock.
 */
fun <T> ReadWriteLock.withReaderLock(block: () -> T): T {
    return this.readLock().withLock(block)
}
