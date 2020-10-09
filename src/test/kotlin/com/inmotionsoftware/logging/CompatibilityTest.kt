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

class CompatibilityTest  {

    @Test
    fun testAllLogLevelsWorkWithOldSchoolLogHandlerWorks() {
        val testLogging = OldSchoolTestLogging()

        val logger = Logger(label="testAllLogLevelsWorkWithOldSchoolLogHandlerWorks", factory=testLogging::make)
        logger.logLevel = LoggerLevel.Trace

        logger.trace("yes: trace")
        logger.debug("yes: debug")
        logger.info("yes: info")
        logger.notice("yes: notice")
        logger.warning("yes: warning")
        logger.error("yes: error")
        logger.critical("yes: critical", location= __location(source="any also with some new argument that isn't propagated"))

        // Please note that the source is _not_ propagated (because the backend doesn't support it).
        testLogging.history.assertExist(level= LoggerLevel.Trace, message="yes: trace", location= __location(source="no source"))
        testLogging.history.assertExist(level= LoggerLevel.Debug, message="yes: debug", location= __location(source="no source"))
        testLogging.history.assertExist(level= LoggerLevel.Info, message="yes: info", location= __location(source="no source"))
        testLogging.history.assertExist(level= LoggerLevel.Notice, message="yes: notice", location= __location(source="no source"))
        testLogging.history.assertExist(level= LoggerLevel.Warning, message="yes: warning", location= __location(source="no source"))
        testLogging.history.assertExist(level= LoggerLevel.Error, message="yes: error", location= __location(source="no source"))
        testLogging.history.assertExist(level= LoggerLevel.Critical, message="yes: critical", location= __location(source="no source"))
    }

}

private class OldSchoolTestLogging {
    private val _config = Config() // shared among loggers
    private val recorder = Recorder() // shared among loggers

    val config: Config get() { return this._config }
    val history: History get() { return this.recorder }

    fun make(label: String): LogHandler {
        return OldSchoolLogHandler(
                    label,
                    this.config,
                    this.recorder,
                    metadata = LoggerMetadata(),
                    logLevel = LoggerLevel.Info
                )
    }
}


private class OldSchoolLogHandler(
    var label: String,
    val config: Config,
    val recorder: Recorder,
    override var metadata: LoggerMetadata,
    override var logLevel: LoggerLevel
): LogHandler {

    fun make(label: String): LogHandler {
        return TestLogHandler(label, this.config, this.recorder)
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
        this.recorder.record(level, metadata, message, source="no source")
    }

    override operator fun get(metadataKey: String): LoggerMetadataValue? {
        return this.metadata[metadataKey]
    }

    override operator fun set(metadataKey: String, value: LoggerMetadataValue) {
        this.metadata[metadataKey] = value
    }

}
