package com.inmotionsoftware.logging

import org.junit.BeforeClass
import org.junit.Test

class LoggingTests {
    companion object {
        @BeforeClass
        @JvmStatic
        fun setup() {
            LoggingSystem.bootstrap { label ->
                StreamLogHandler.standardOutput(label)
            }
        }
    }

    @Test
    fun test() {
        val logger = Logger("test")
        TODO("To implement")
    }

}
