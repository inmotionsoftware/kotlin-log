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
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.random.nextInt
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

class MDCTest {

    @Test
    fun test1() {
        // bootstrap with our test logger
        val logging = TestLogging()
        LoggingSystem.bootstrapInternal(logging::make)

        // run the program
        MDC.global["foo"] = "bar".asLoggerMetadataValue()
        val executor = Executors.newCachedThreadPool()
        val range = 5..10
        val e = CountDownLatch(range.count())

        for (r in range) {
            executor.execute {
                val add = Random.nextInt(10 until 1000)
                val remove = Random.nextInt(0 until add-1)
                for (i in 0 until add) {
                    MDC.global["key-$i"] = "value-$i".asLoggerMetadataValue()
                }
                for (i in 0 until remove) {
                    MDC.global["key-$i"] = null
                }
                assertEquals("expected number of entries to match", add-remove, MDC.global.metadata.size)
                for (i in remove+1 until add) {
                    assertNotNull("expecting value for key-$i", MDC.global["key-$i"])
                }
                for (i in 0 until remove) {
                    assertNull("not expecting value for key-$i", MDC.global["key-$i"])
                }
                MDC.global.clear()
                e.countDown()
            }
        }
        e.await(2, TimeUnit.SECONDS)
        assertEquals("expecting to find top items", MDC.global["foo"], "bar".asLoggerMetadataValue())
        MDC.global["foo"] = null
        assertTrue("MDC should be empty", MDC.global.metadata.isEmpty())
    }

}
