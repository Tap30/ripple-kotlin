package com.tapsioss.ripple.core.adapters
/**
 * No-op logger adapter for silent operation.
 */
class NoOpLoggerAdapter : LoggerAdapter {
    override fun debug(message: String, vararg args: Any?) {}
    override fun info(message: String, vararg args: Any?) {}
    override fun warn(message: String, vararg args: Any?) {}
    override fun error(message: String, vararg args: Any?) {}
}
