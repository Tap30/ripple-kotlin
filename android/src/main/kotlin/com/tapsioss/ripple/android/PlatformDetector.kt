package com.tapsioss.ripple.android

import android.content.Context
import android.os.Build
import com.tapsioss.ripple.core.DeviceInfo
import com.tapsioss.ripple.core.OsInfo
import com.tapsioss.ripple.core.Platform

/**
 * Detects Android platform information
 */
class PlatformDetector(private val context: Context) {
    
    fun getPlatform(): Platform.Android {
        return Platform.Android(
            device = getDeviceInfo(),
            os = getOsInfo()
        )
    }

    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            name = "${Build.MANUFACTURER} ${Build.MODEL}",
            version = Build.MODEL
        )
    }

    private fun getOsInfo(): OsInfo {
        return OsInfo(
            name = "Android",
            version = Build.VERSION.RELEASE
        )
    }
}
