package com.tapsioss.ripple.core.adapters
/**
 * Default console logger adapter with configurable level.
 */
class ConsoleLoggerAdapter(
    private val level: LogLevel = LogLevel.WARN
) : LoggerAdapter {
    
    override fun debug(message: String, vararg args: Any?) {
        if (level <= LogLevel.DEBUG) {
            println("[DEBUG] Ripple: $message")
        }
    }
    
    override fun info(message: String, vararg args: Any?) {
        if (level <= LogLevel.INFO) {
            println("[INFO] Ripple: $message")
        }
    }
    
    override fun warn(message: String, vararg args: Any?) {
        if (level <= LogLevel.WARN) {
            println("[WARN] Ripple: $message")
        }
    }
    
    override fun error(message: String, vararg args: Any?) {
        if (level <= LogLevel.ERROR) {
            System.err.println("[ERROR] Ripple: $message")
        }
    }
}