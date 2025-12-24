package com.tapsioss.ripple.spring.adapters

import com.tapsioss.ripple.core.adapters.LogLevel
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * SLF4J-based logger adapter for Spring
 */
class Slf4jLoggerAdapter @JvmOverloads constructor(
    private val logLevel: LogLevel = LogLevel.WARN,
    private val logger: Logger = LoggerFactory.getLogger("Ripple")
) : LoggerAdapter {
    
    override fun debug(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.DEBUG && logger.isDebugEnabled) {
            logger.debug(message, *args)
        }
    }

    override fun info(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.INFO && logger.isInfoEnabled) {
            logger.info(message, *args)
        }
    }

    override fun warn(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.WARN && logger.isWarnEnabled) {
            logger.warn(message, *args)
        }
    }

    override fun error(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.ERROR && logger.isErrorEnabled) {
            logger.error(message, *args)
        }
    }
}
