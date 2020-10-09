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

class GlobalLoggerTest {
    @Test
    fun test1() {
        // bootstrap with our test logging impl
        val logging = TestLogging()
        LoggingSystem.bootstrapInternal(logging::make)

        // change test logging config to log traces and above
        logging.config.set(value = LoggerLevel.Debug)

        // run our program
        Struct1().doSomething()

        // test results
        logging.history.assertExist(level = LoggerLevel.Debug, message = "Struct1::doSomething")
        logging.history.assertExist(level = LoggerLevel.Debug, message = "Struct1::doSomethingElse")
        logging.history.assertExist(level = LoggerLevel.Info, message = "Struct2::doSomething")
        logging.history.assertExist(level = LoggerLevel.Info, message = "Struct2::doSomethingElse")
        logging.history.assertExist(level = LoggerLevel.Error, message = "Struct3::doSomething")
        logging.history.assertExist(level = LoggerLevel.Error, message = "Struct3::doSomethingElse",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertExist(level = LoggerLevel.Warning,
                message = "Struct3::doSomethingElseAsync",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertExist(level = LoggerLevel.Info,
                message = "TestLibrary::doSomething",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertExist(level = LoggerLevel.Info,
                message = "TestLibrary::doSomethingAsync",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertExist(level = LoggerLevel.Debug,
                message = "Struct3::doSomethingElse::Local",
                metadata = mutableMapOf("baz" to "qux".asLoggerMetadataValue()))
        logging.history.assertExist(level = LoggerLevel.Debug, message = "Struct3::doSomething::end")
        logging.history.assertExist(level = LoggerLevel.Debug, message = "Struct2::doSomethingElse::end")
        logging.history.assertExist(level = LoggerLevel.Debug, message = "Struct1::doSomethingElse::end")
        logging.history.assertExist(level = LoggerLevel.Debug, message = "Struct1::doSomething::end")
    }

    @Test
    fun test2() {
        // bootstrap with our test logging impl
        val logging = TestLogging()
        LoggingSystem.bootstrapInternal(logging::make)

        // change test logging config to log errors and above
        logging.config.set(value = LoggerLevel.Error)

        // run our program
        Struct1().doSomething()

        // test results
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct1::doSomething")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct1::doSomethingElse")
        logging.history.assertNotExist(level = LoggerLevel.Info, message = "Struct2::doSomething")
        logging.history.assertNotExist(level = LoggerLevel.Info, message = "Struct2::doSomethingElse")
        logging.history.assertExist(level = LoggerLevel.Error, message = "Struct3::doSomething")
        logging.history.assertExist(level = LoggerLevel.Error,
                message = "Struct3::doSomethingElse",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertNotExist(level = LoggerLevel.Warning,
                message = "Struct3::doSomethingElseAsync",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertNotExist(level = LoggerLevel.Info,
                message = "TestLibrary::doSomething",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertNotExist(level = LoggerLevel.Info,
                message = "TestLibrary::doSomethingAsync",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertNotExist(level = LoggerLevel.Debug,
                message = "Struct3::doSomethingElse::Local",
                metadata = mutableMapOf("baz" to "qux".asLoggerMetadataValue()))
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct3::doSomethingElse::end")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct3::doSomething::end")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct2::doSomethingElse::end")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct1::doSomethingElse::end")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct1::doSomething::end")
    }

    @Test
    fun test3() {
        // bootstrap with our test logging impl
        val logging = TestLogging()
        LoggingSystem.bootstrapInternal(logging::make)

        // change test logging config
        logging.config.set(value = LoggerLevel.Warning)
        logging.config["GlobalLoggerTest::Struct2"] = LoggerLevel.Info
        logging.config["TestLibrary"] = LoggerLevel.Debug

        // run our program
        Struct1().doSomething()
        // test results
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct1::doSomething")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct1::doSomethingElse")
        logging.history.assertExist(level = LoggerLevel.Info, message = "Struct2::doSomething")
        logging.history.assertExist(level = LoggerLevel.Info, message = "Struct2::doSomethingElse")
        logging.history.assertExist(level = LoggerLevel.Error, message = "Struct3::doSomething")
        logging.history.assertExist(level = LoggerLevel.Error,
                message = "Struct3::doSomethingElse",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertExist(level = LoggerLevel.Warning,
                message = "Struct3::doSomethingElseAsync",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertExist(level = LoggerLevel.Info,
                message = "TestLibrary::doSomething",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertExist(level = LoggerLevel.Info,
                message = "TestLibrary::doSomethingAsync",
                metadata = mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging.history.assertNotExist(level = LoggerLevel.Debug,
                message = "Struct3::doSomethingElse::Local",
                metadata = mutableMapOf("baz" to "qux".asLoggerMetadataValue()))
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct3::doSomethingElse::end")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct3::doSomething::end")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct2::doSomethingElse::end")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct1::doSomethingElse::end")
        logging.history.assertNotExist(level = LoggerLevel.Debug, message = "Struct1::doSomething::end")
    }

    private class Struct1(val logger: Logger = Logger(label = "GlobalLoggerTest::Struct1")) {
        fun doSomething() {
            this.logger.debug("Struct1::doSomething")
            this.doSomethingElse()
            this.logger.debug("Struct1::doSomething::end")
        }

        private fun doSomethingElse() {
            this.logger.debug("Struct1::doSomethingElse")
            Struct2().doSomething()
            this.logger.debug("Struct1::doSomethingElse::end")
        }
    }

    private class Struct2(val logger: Logger = Logger(label = "GlobalLoggerTest::Struct2")) {
        fun doSomething() {
            this.logger.info("Struct2::doSomething")
            this.doSomethingElse()
            this.logger.debug("Struct2::doSomething::end")
        }

        private fun doSomethingElse() {
            this.logger.info("Struct2::doSomethingElse")
            Struct3().doSomething()
            this.logger.debug("Struct2::doSomethingElse::end")
        }
    }

    private class Struct3(
            private val logger: Logger = Logger(label = "GlobalLoggerTest::Struct3"),
            private val executor: Executor = Executors.newCachedThreadPool()
    ) {
        fun doSomething() {
            this.logger.error("Struct3::doSomething")
            this.doSomethingElse()
            this.logger.debug("Struct3::doSomething::end")
        }

        private fun doSomethingElse() {
            MDC.global["foo"] = LoggerMetadataValue.String("bar")
            this.logger.error("Struct3::doSomethingElse")

            val e = CountDownLatch(1)
            val loggingMetadata = MDC.global.metadata
            this.executor.execute {
                MDC.global.with(metadata = loggingMetadata) {
                    this.logger.warning("Struct3::doSomethingElseAsync")
                    val library = TestLibrary()
                    library.doSomething()
                    library.doSomethingAsync {
                        e.countDown()
                    }
                }
            }
            e.await(2, TimeUnit.SECONDS)
            MDC.global["foo"] = null

            // only effects this local logger instance
            val l = Logger(label = "LocalLoggerTest::Struct3")
            l["baz"] = LoggerMetadataValue.String("qux")
            l.debug("Struct3::doSomethingElse::Local")

            this.logger.debug("Struct3::doSomethingElse::end")
        }

    }
}
