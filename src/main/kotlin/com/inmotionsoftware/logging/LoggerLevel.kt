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
 *  The log level.
 * 
 *  Log levels are ordered by their severity, with `Trace` being the least severe and
 *  `Critical` being the most severe.
 */
enum class LoggerLevel {
    /**
     *  Appropriate for messages that contain information normally of use only when
     *  tracing the execution of a program.
     */
    Trace,

    /**
     *  Appropriate for messages that contain information normally of use only when
     *  debugging a program.
     */
    Debug,

    /**
     *  Appropriate for informational messages.
     */
    Info,

    /**
     *  Appropriate for conditions that are not error conditions, but that may require special handling.
     */
    Notice,

    /**
     *  Appropriate for messages that are not error conditions, but more severe than `Notice`.
     */
    Warning,

    /**
     *  Appropriate for error conditions.
     */
    Error,

    /**
     *  Appropriate for critical error conditions that usually require immediate attention.
     * 
     *  When a `Critical` message is logged, the logging backend (`LogHandler`) is free to perform
     *  more heavy-weight operations to capture system state (such as capturing stack traces) to facilitate
     *  debugging.
     */
    Critical
}
