/***************************************************************************
 * This source file is part of the kotlin-log open source project.         *
 *                                                                         *
 * Copyright (c) 2020-present, InMotion Software and the project authors   *
 * Licensed under the MIT License                                          *
 *                                                                         *
 * See LICENSE.txt for license information                                 *
 ***************************************************************************/

package com.inmotionsoftware.logging

import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LocalLoggingTest {

    @Test
    fun test1() {
        // bootstrap with our test logging impl
        val logging = TestLogging()
        LoggingSystem.bootstrapInternal(logging::make)

        // change test logging config to log traces and above
        logging.config.set(value=LoggerLevel.Debug)

        // run our program
        val context = Context()
        Struct1().doSomething(context)

        // test results
        logging.history.assertExist(level=LoggerLevel.Debug,
                                    message="Struct1::doSomething",
                                    location=__location(source="LoggingTests"))
        logging.history.assertNotExist(level=LoggerLevel.Debug, message="Struct1::doSomethingElse")
        logging.history.assertExist(level=LoggerLevel.Info, message="Struct2::doSomething")
        logging.history.assertExist(level=LoggerLevel.Info, message="Struct2::doSomethingElse")
        logging.history.assertExist(level=LoggerLevel.Error,
                                    message="Struct3::doSomething",
                                    metadata=mutableMapOf("bar" to "baz".asLoggerMetadataValue()))
        logging.history.assertExist(level=LoggerLevel.Error,
                                    message="Struct3::doSomethingElse",
                                    metadata= mutableMapOf("bar" to "baz".asLoggerMetadataValue()))
        logging.history.assertExist(level=LoggerLevel.Warning,
                                    message="Struct3::doSomethingElseAsync",
                                    metadata= mutableMapOf("bar" to "baz".asLoggerMetadataValue()))
        logging.history.assertExist(level=LoggerLevel.Info, message="TestLibrary::doSomething")
        logging.history.assertExist(level=LoggerLevel.Info, message="TestLibrary::doSomethingAsync")
        logging.history.assertExist(level=LoggerLevel.Debug,
                                    message="Struct3::doSomethingElse::Local",
                                    metadata= mutableMapOf("baz" to "qux".asLoggerMetadataValue()))
        logging.history.assertExist(level=LoggerLevel.Debug,
                                    message="Struct3::doSomethingElse::end",
                                    metadata= mutableMapOf("bar" to "baz".asLoggerMetadataValue()))
        logging.history.assertExist(level=LoggerLevel.Debug,
                                    message="Struct3::doSomething::end",
                                    metadata= mutableMapOf("bar" to "baz".asLoggerMetadataValue()))
        logging.history.assertExist(level=LoggerLevel.Debug, message="Struct1::doSomethingElse::end")
        logging.history.assertExist(level=LoggerLevel.Debug, message="Struct1::doSomething::end")
    }

    @Test
    fun test2() {
        // bootstrap with our test logging impl
        val logging = TestLogging()
        LoggingSystem.bootstrapInternal(logging::make)

        // change test logging config to log errors and above
        logging.config.set(value=LoggerLevel.Error)

        // run our program
        val context = Context()
        Struct1().doSomething(context)
        // test results
        logging.history.assertNotExist(level=LoggerLevel.Debug, message="Struct1::doSomething") // global context
        logging.history.assertNotExist(level=LoggerLevel.Debug, message="Struct1::doSomethingElse") // global context
        logging.history.assertExist(level=LoggerLevel.Info, message="Struct2::doSomething") // local context
        logging.history.assertExist(level=LoggerLevel.Info, message="Struct2::doSomethingElse") // local context
        logging.history.assertExist(level=LoggerLevel.Error,
                                    message="Struct3::doSomething",
                                    metadata= mutableMapOf("bar" to "baz".asLoggerMetadataValue())) // local context
        logging.history.assertExist(level=LoggerLevel.Error,
                                    message="Struct3::doSomethingElse",
                                    metadata= mutableMapOf("bar" to "baz".asLoggerMetadataValue())) // local context
        logging.history.assertExist(level=LoggerLevel.Warning,
                                    message="Struct3::doSomethingElseAsync",
                                    metadata= mutableMapOf("bar" to "baz".asLoggerMetadataValue())) // local context
        logging.history.assertNotExist(level=LoggerLevel.Info, message="TestLibrary::doSomething") // global context
        logging.history.assertNotExist(level=LoggerLevel.Info, message="TestLibrary::doSomethingAsync") // global context
        logging.history.assertExist(level=LoggerLevel.Debug,
                                    message="Struct3::doSomethingElse::Local",
                                    metadata= mutableMapOf("baz" to "qux".asLoggerMetadataValue())) // hyper local context
        logging.history.assertExist(level=LoggerLevel.Debug,
                                    message="Struct3::doSomethingElse::end",
                                    metadata= mutableMapOf("bar" to "baz".asLoggerMetadataValue())) // local context
        logging.history.assertExist(level=LoggerLevel.Debug, message="Struct2::doSomethingElse::end") // local context
        logging.history.assertNotExist(level=LoggerLevel.Debug, message="Struct1::doSomething::end") // global context
    }

    //  systems that follow the context pattern  need to implement something like this
    private data class Context(val logger: Logger = Logger(label = "LocalLoggerTest::ContextLogger")) {
        // since logger is a value type, we can reuse our copy to manage logLevel
        var logLevel: LoggerLevel
            get() {
                return this.logger.logLevel
            }
            set(value) {
                this.logger.logLevel = value
            }

        // since logger is a value type, we can reuse our copy to manage metadata
        operator fun get(metadataKey: String): LoggerMetadataValue? {
            return this.logger[metadataKey]
        }

        operator fun set(metadataKey: String, value: LoggerMetadataValue) {
            this.logger[metadataKey] = value
        }
    }

    private class Struct1 {
        fun doSomething(context: Context) {
            context.logger.debug("Struct1::doSomething", location=__location(source="LoggingTests"))
            this.doSomethingElse(context)
            context.logger.debug("Struct1::doSomething::end")
        }

        private fun doSomethingElse(context: Context) {
            val newContext = Context()
            newContext.logger.logLevel = LoggerLevel.Warning
            newContext.logger.debug("Struct1::doSomethingElse")
            Struct2().doSomething(newContext)
            context.logger.debug("Struct1::doSomethingElse::end")
        }
    }

    private class Struct2 {
        fun doSomething(context: Context) {
            val c = Context()
            c.logLevel = LoggerLevel.Info // only effects from this point on
            c.logger.info("Struct2::doSomething")
            this.doSomethingElse(context = c)
            c.logger.debug("Struct2::doSomething::end")
        }

        private fun doSomethingElse(context: Context) {
            val c = Context()
            c.logLevel = LoggerLevel.Debug // only effects from this point on
            c.logger.info("Struct2::doSomethingElse")
            Struct3().doSomething(context = c)
            c.logger.debug("Struct2::doSomethingElse::end")
        }
    }

    private class Struct3 {
        private val executor: Executor = Executors.newCachedThreadPool()

        fun doSomething(context: Context) {
            val c = Context()
            c.logLevel = LoggerLevel.Debug // only effects from this point on
            c["bar"] = "baz".asLoggerMetadataValue()
            c.logger.error("Struct3::doSomething")
            this.doSomethingElse(context = c)
            c.logger.debug("Struct3::doSomething::end")
        }

        private fun doSomethingElse(context: Context) {
            context.logger.error("Struct3::doSomethingElse")

            val e = CountDownLatch(1)
            this.executor.execute {
                context.logger.warning("Struct3::doSomethingElseAsync")
                val library = TestLibrary()
                library.doSomething()
                library.doSomethingAsync {
                    e.countDown()
                }
            }
            e.await(2, TimeUnit.SECONDS)

            // only effects the logger instance
            val l = Logger(label = "LocalLoggingTest::Struct3")
            l.logLevel = LoggerLevel.Debug
            l["baz"] = "qux".asLoggerMetadataValue()
            l.debug("Struct3::doSomethingElse::Local")

            context.logger.debug("Struct3::doSomethingElse::end")
        }
    }

}