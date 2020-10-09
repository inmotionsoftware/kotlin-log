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
import org.junit.Assert.*

class LoggingTest {

    @Test
    fun testClosure() {
        // bootstrap with our test logging impl
        val logging = TestLogging()
        LoggingSystem.bootstrapInternal(logging::make)

        val logger = Logger(label="test")
        logger.logLevel = LoggerLevel.Info
        logger.log(
                level=LoggerLevel.Debug,
                message= {
                    assertTrue("debug should not be called", false)
                    "debug".asLoggerMessage()
                }, file = null, function = null, line = null)

        logger.trace({
                assertTrue("trace should not be called", false)
                "trace".asLoggerMessage()
            })

        logger.debug({
                assertTrue("debug should not be called", false)
                "debug".asLoggerMessage()
            })

        logger.info("info")
        logger.warning("warning")
        logger.error("error")

        assertEquals("expected number of entries to match", 3, logging.history.entries.size)
        logging.history.assertNotExist(level=LoggerLevel.Debug, message="trace")
        logging.history.assertNotExist(level=LoggerLevel.Debug, message="debug")
        logging.history.assertExist(level=LoggerLevel.Info, message="info")
        logging.history.assertExist(level=LoggerLevel.Warning, message="warning")
        logging.history.assertExist(level=LoggerLevel.Error, message="error")
    }

    @Test
    fun testMultiplex() {
        // bootstrap with our test logging impl
        val logging1 = TestLogging()
        val logging2 = TestLogging()
        LoggingSystem.bootstrapInternal {
            MultiplexLogHandler(listOf(logging1.make(label=it), logging2.make(label=it)))
        }

        val logger = Logger(label="test")
        logger.logLevel = LoggerLevel.Warning
        logger.info("hello world?")

        logger["foo"] = "bar".asLoggerMetadataValue()
        logger.warning("hello world!")

        logging1.history.assertNotExist(level=LoggerLevel.Info, message="hello world?")
        logging2.history.assertNotExist(level=LoggerLevel.Info, message="hello world?")
        logging1.history.assertExist(level=LoggerLevel.Warning,
                                     message="hello world!",
                                     metadata= mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
        logging2.history.assertExist(level=LoggerLevel.Warning,
                                     message="hello world!",
                                     metadata= mutableMapOf("foo" to "bar".asLoggerMetadataValue()))
    }

    @Test
    fun testMultiplexLogHandlerWithVariousLogLevels() {
        val logging1 = TestLogging()
        val logging2 = TestLogging()

        val logger1 = logging1.make("1")
        logger1.logLevel = LoggerLevel.Info

        val logger2 = logging2.make("2")
        logger2.logLevel = LoggerLevel.Debug

        LoggingSystem.bootstrapInternal {
            MultiplexLogHandler(listOf(logger1, logger2))
        }

        val multiplexLogger = Logger("test")
        multiplexLogger.trace("trace")
        multiplexLogger.debug("debug")
        multiplexLogger.info("info")
        multiplexLogger.warning("warning")

        logging1.history.assertNotExist(level=LoggerLevel.Trace, message="trace")
        logging1.history.assertNotExist(level=LoggerLevel.Debug, message="debug")
        logging1.history.assertExist(level=LoggerLevel.Info, message="info")
        logging1.history.assertExist(level=LoggerLevel.Warning, message="warning")

        logging2.history.assertNotExist(level=LoggerLevel.Trace, message="trace")
        logging2.history.assertExist(level=LoggerLevel.Debug, message="debug")
        logging2.history.assertExist(level=LoggerLevel.Info, message="info")
        logging2.history.assertExist(level=LoggerLevel.Warning, message="warning")
    }

    @Test
    fun testMultiplexLogHandlerNeedNotMaterializeValuesMultipleTimes() {
        val logging1 = TestLogging()
        val logging2 = TestLogging()

        val logger1 = logging1.make("1")
        logger1.logLevel = LoggerLevel.Info

        val logger2 = logging2.make("2")
        logger2.logLevel = LoggerLevel.Info

        LoggingSystem.bootstrapInternal {
            MultiplexLogHandler(listOf(logger1, logger2))
        }

        var messageMaterializations = 0
        var metadataMaterializations = 0

        val multiplexLogger = Logger("test")
        multiplexLogger.info(
                message= {
                    messageMaterializations += 1
                    "info".asLoggerMessage()
                },
                metadata = {
                    metadataMaterializations += 1
                    LoggerMetadata()
                }
            )

        logging1.history.assertExist(level=LoggerLevel.Info, message="info")
        logging2.history.assertExist(level=LoggerLevel.Info, message="info")

        assertEquals(messageMaterializations, 1)
        assertEquals(metadataMaterializations, 1)
    }

    @Test
    fun testMultiplexLogHandlerMetadata_settingMetadataThroughToUnderlyingHandlers() {
        val logging1 = TestLogging()
        val logging2 = TestLogging()

        val logger1 = logging1.make("1")
        logger1["one"] = "111".asLoggerMetadataValue()
        logger1["in"] = "in-1".asLoggerMetadataValue()

        val logger2 = logging2.make("2")
        logger2["two"] = "222".asLoggerMetadataValue()
        logger2["in"] = "in-2".asLoggerMetadataValue()

        LoggingSystem.bootstrapInternal {
            MultiplexLogHandler(listOf(logger1, logger2))
        }

        val multiplexLogger = Logger("test")

        // each logs its own metadata
        multiplexLogger.info("info")
        logging1.history.assertExist(level=LoggerLevel.Info,
                                     message="info",
                                     metadata= mutableMapOf(
                                                "one" to "111".asLoggerMetadataValue(),
                                                "in" to "in-1".asLoggerMetadataValue()))
        logging2.history.assertExist(level=LoggerLevel.Info,
                                     message="info",
                                     metadata= mutableMapOf(
                                                    "two" to "222".asLoggerMetadataValue(),
                                                    "in" to "in-2".asLoggerMetadataValue()))

        // if modified, change applies to both underlying handlers
        multiplexLogger["new"] = "new".asLoggerMetadataValue()
        multiplexLogger.info("info")
        logging1.history.assertExist(level=LoggerLevel.Info,
                                    message="info",
                                    metadata= mutableMapOf(
                                                "one" to "111".asLoggerMetadataValue(),
                                                "in" to "in-1".asLoggerMetadataValue(),
                                                "new" to "new".asLoggerMetadataValue()))

        logging2.history.assertExist(level=LoggerLevel.Info,
                                    message="info",
                                    metadata= mutableMapOf(
                                                "two" to "222".asLoggerMetadataValue(),
                                                "in" to "in-2".asLoggerMetadataValue(),
                                                "new" to "new".asLoggerMetadataValue()))

        // overriding an existing value works the same way as adding a new one
        multiplexLogger["in"] = "multi".asLoggerMetadataValue()
        multiplexLogger.info("info")

        logging1.history.assertExist(level=LoggerLevel.Info,
                                    message="info",
                                    metadata= mutableMapOf(
                                                "one" to "111".asLoggerMetadataValue(),
                                                "in" to "multi".asLoggerMetadataValue(),
                                                "new" to "new".asLoggerMetadataValue()))

        logging2.history.assertExist(level=LoggerLevel.Info, message="info",
                                    metadata= mutableMapOf(
                                                "two" to "222".asLoggerMetadataValue(),
                                                "in" to "multi".asLoggerMetadataValue(),
                                                "new" to "new".asLoggerMetadataValue()))
    }

    @Test
    fun testMultiplexLogHandlerMetadata_readingHandlerMetadata() {
        val logging1 = TestLogging()
        val logging2 = TestLogging()

        val logger1 = logging1.make("1")
        logger1["one"] = "111".asLoggerMetadataValue()
        logger1["in"] = "in-1".asLoggerMetadataValue()
        val logger2 = logging2.make("2")
        logger2["two"] = "222".asLoggerMetadataValue()
        logger2["in"] = "in-2".asLoggerMetadataValue()

        LoggingSystem.bootstrapInternal {
            MultiplexLogHandler(listOf(logger1, logger2))
        }

        val multiplexLogger = Logger("test")

        assertEquals(multiplexLogger.handler.metadata, mutableMapOf(
                "one" to "111".asLoggerMetadataValue(),
                "two" to "222".asLoggerMetadataValue(),
                "in" to "in-1".asLoggerMetadataValue()
            ))
    }

    @Test
    fun testDictionaryMetadata() {
        val testLogging = TestLogging()
        LoggingSystem.bootstrapInternal(testLogging::make)

        val logger = Logger("testDictionaryMetadata")
        logger["foo"] = LoggerMetadataValue.Dictionary(mutableMapOf("bar" to "buz".asLoggerMetadataValue()))
        logger["empty-dict"] = LoggerMetadataValue.Dictionary(LoggerMetadata())
        logger["nested-dict"] = LoggerMetadataValue.Dictionary(
                        mutableMapOf("l1key" to LoggerMetadataValue.Dictionary(
                                        mutableMapOf("l2key" to LoggerMetadataValue.Dictionary(
                                                                mutableMapOf("l3key" to "l3value".asLoggerMetadataValue()))))))
        logger.info("hello world!")
        testLogging.history.assertExist(
                                level=LoggerLevel.Info,
                                message="hello world!",
                                metadata= mutableMapOf("foo" to LoggerMetadataValue.Dictionary(mutableMapOf("bar" to "buz".asLoggerMetadataValue())),
                                                        "empty-dict" to LoggerMetadataValue.Dictionary(LoggerMetadata()),
                                                        "nested-dict" to LoggerMetadataValue.Dictionary(
                                                                mutableMapOf("l1key" to LoggerMetadataValue.Dictionary(
                                                                        mutableMapOf("l2key" to LoggerMetadataValue.Dictionary(
                                                                                mutableMapOf("l3key" to "l3value".asLoggerMetadataValue()))))))))
    }

    @Test
    fun testListMetadata() {
        val testLogging = TestLogging()
        LoggingSystem.bootstrapInternal(testLogging::make)

        val logger = Logger("testListMetadata")
        logger["foo"] = LoggerMetadataValue.Array(listOf("bar".asLoggerMetadataValue(), "buz".asLoggerMetadataValue()))
        logger["empty-list"] = LoggerMetadataValue.Array(emptyList())
        logger["nested-list"] = LoggerMetadataValue.Array(
                                    listOf(
                                            "l1str".asLoggerMetadataValue(),
                                            LoggerMetadataValue.Array(listOf("l2str1".asLoggerMetadataValue(), "l2str2".asLoggerMetadataValue()))))

        logger.info("hello world!")
        testLogging.history.assertExist(
                                level=LoggerLevel.Info,
                                message="hello world!",
                                metadata= mutableMapOf("foo" to LoggerMetadataValue.Array(listOf("bar".asLoggerMetadataValue(), "buz".asLoggerMetadataValue())),
                                                        "empty-list" to LoggerMetadataValue.Array(emptyList()),
                                                        "nested-list" to LoggerMetadataValue.Array(listOf("l1str".asLoggerMetadataValue(),
                                                                LoggerMetadataValue.Array(listOf("l2str1".asLoggerMetadataValue(), "l2str2".asLoggerMetadataValue()))))))
    }

    @Test
    fun testCustomFactory() {
        val customHandler: LogHandler = object: LogHandler {
            override fun log(level: LoggerLevel, message: LoggerMessage, metadata: LoggerMetadata?, source: String?, file: String?, function: String?, line: Int?) {
            }

            override fun get(metadataKey: String): LoggerMetadataValue? {
                return null
            }

            override fun set(metadataKey: String, value: LoggerMetadataValue) {
            }

            override var metadata: LoggerMetadata
                get() = LoggerMetadata()
                set(value) {}

            override var logLevel: LoggerLevel
                get() = LoggerLevel.Info
                set(value) {}
        }

        val logger1 = Logger("foo")
        assertFalse("expected non-custom log handler", logger1.handler == customHandler)
        val logger2 = Logger("foo", factory={ customHandler })
        assertTrue("expected custom log handler", logger2.handler == customHandler)
    }

    @Test
    fun testAllLogLevelsExceptCriticalCanBeBlocked() {
        val testLogging = TestLogging()
        LoggingSystem.bootstrapInternal(testLogging::make)

        val logger = Logger("testAllLogLevelsExceptCriticalCanBeBlocked")
        logger.logLevel = LoggerLevel.Critical

        logger.trace("no")
        logger.debug("no")
        logger.info("no")
        logger.notice("no")
        logger.warning("no")
        logger.error("no")
        logger.critical("yes: critical")

        testLogging.history.assertNotExist(level=LoggerLevel.Trace, message="no")
        testLogging.history.assertNotExist(level=LoggerLevel.Debug, message="no")
        testLogging.history.assertNotExist(level=LoggerLevel.Info, message="no")
        testLogging.history.assertNotExist(level=LoggerLevel.Notice, message="no")
        testLogging.history.assertNotExist(level=LoggerLevel.Warning, message="no")
        testLogging.history.assertNotExist(level=LoggerLevel.Error, message="no")
        testLogging.history.assertExist(level=LoggerLevel.Critical, message="yes: critical")
    }

    @Test
    fun testAllLogLevelsWork() {
        val testLogging = TestLogging()
        LoggingSystem.bootstrapInternal(testLogging::make)

        val logger = Logger("testAllLogLevelsWork")
        logger.logLevel = LoggerLevel.Trace

        logger.trace("yes: trace")
        logger.debug("yes: debug")
        logger.info("yes: info")
        logger.notice("yes: notice")
        logger.warning("yes: warning")
        logger.error("yes: error")
        logger.critical("yes: critical")

        testLogging.history.assertExist(level=LoggerLevel.Trace, message="yes: trace")
        testLogging.history.assertExist(level=LoggerLevel.Debug, message="yes: debug")
        testLogging.history.assertExist(level=LoggerLevel.Info, message="yes: info")
        testLogging.history.assertExist(level=LoggerLevel.Notice, message="yes: notice")
        testLogging.history.assertExist(level=LoggerLevel.Warning, message="yes: warning")
        testLogging.history.assertExist(level=LoggerLevel.Error, message="yes: error")
        testLogging.history.assertExist(level=LoggerLevel.Critical, message="yes: critical")
    }

    @Test
    fun testMultiplexerIsValue() {
        LoggingSystem.bootstrapInternal {
            MultiplexLogHandler(listOf(StreamLogHandler.standardOutput("x"), StreamLogHandler.standardOutput("y")))
        }

        val logger1 = Logger("foo").apply {
            this.logLevel = LoggerLevel.Debug
            this["only-on"] = "first".asLoggerMetadataValue()
        }

        assertEquals(LoggerLevel.Debug, logger1.logLevel)
        val logger2 = Logger("foo").apply {
            this.logLevel = LoggerLevel.Error
            this["only-on"] = "second".asLoggerMetadataValue()
        }

        assertEquals(LoggerLevel.Error, logger2.logLevel)
        assertEquals(LoggerLevel.Debug, logger1.logLevel)
        assertEquals("first".asLoggerMetadataValue(), logger1["only-on"])
        assertEquals("second".asLoggerMetadataValue(), logger2["only-on"])
        logger1.error("hey")
    }

    @Test
    fun testLogLevelOrdering() {
        assertTrue(LoggerLevel.Trace < LoggerLevel.Debug)
        assertTrue(LoggerLevel.Trace < LoggerLevel.Info)
        assertTrue(LoggerLevel.Trace < LoggerLevel.Notice)
        assertTrue(LoggerLevel.Trace < LoggerLevel.Warning)
        assertTrue(LoggerLevel.Trace < LoggerLevel.Error)
        assertTrue(LoggerLevel.Trace < LoggerLevel.Critical)
        assertTrue(LoggerLevel.Debug < LoggerLevel.Info)
        assertTrue(LoggerLevel.Debug < LoggerLevel.Notice)
        assertTrue(LoggerLevel.Debug < LoggerLevel.Warning)
        assertTrue(LoggerLevel.Debug < LoggerLevel.Error)
        assertTrue(LoggerLevel.Debug < LoggerLevel.Critical)
        assertTrue(LoggerLevel.Info < LoggerLevel.Notice)
        assertTrue(LoggerLevel.Info < LoggerLevel.Warning)
        assertTrue(LoggerLevel.Info < LoggerLevel.Error)
        assertTrue(LoggerLevel.Info < LoggerLevel.Critical)
        assertTrue(LoggerLevel.Notice < LoggerLevel.Warning)
        assertTrue(LoggerLevel.Notice < LoggerLevel.Error)
        assertTrue(LoggerLevel.Notice < LoggerLevel.Critical)
        assertTrue(LoggerLevel.Warning < LoggerLevel.Error)
        assertTrue(LoggerLevel.Warning < LoggerLevel.Critical)
        assertTrue(LoggerLevel.Error < LoggerLevel.Critical)
    }

}
