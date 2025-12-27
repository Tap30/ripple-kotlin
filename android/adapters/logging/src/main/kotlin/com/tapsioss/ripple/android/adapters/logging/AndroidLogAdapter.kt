package com.tapsioss.ripple.android.adapters.logging

import android.util.Log
import com.tapsioss.ripple.core.adapters.LogLevel
import com.tapsioss.ripple.core.adapters.LoggerAdapter

/**
 * Android Log-based logger adapter
 */
class AndroidLogAdapter(
    private val tag: String = "Ripple",
    private val logLevel: LogLevel = LogLevel.WARN
) : LoggerAdapter {
    
    override fun debug(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.DEBUG) {
            Log.d(tag, formatMessage(message, *args))
        }
    }

    override fun info(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.INFO) {
            Log.i(tag, formatMessage(message, *args))
        }
    }

    override fun warn(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.WARN) {
            Log.w(tag, formatMessage(message, *args))
        }
    }

    override fun error(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.ERROR) {
            Log.e(tag, formatMessage(message, *args))
        }
    }

    private fun formatMessage(message: String, vararg args: Any?): String {
        return if (args.isNotEmpty()) {
            String.format(message, *args)
        } else {
            message
        }
    }
}
