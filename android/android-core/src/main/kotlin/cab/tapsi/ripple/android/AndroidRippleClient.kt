package cab.tapsi.ripple.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import cab.tapsi.ripple.core.AppState
import cab.tapsi.ripple.core.AppStateChangedPayload
import cab.tapsi.ripple.core.DefaultRippleEvent
import cab.tapsi.ripple.core.DefaultRippleMetadata
import cab.tapsi.ripple.core.DeviceInfo
import cab.tapsi.ripple.core.Platform
import cab.tapsi.ripple.core.RippleClient
import cab.tapsi.ripple.core.RippleConfig
import cab.tapsi.ripple.core.RippleEvent
import cab.tapsi.ripple.core.RippleMetadata
import cab.tapsi.ripple.core.ScreenPayload

/**
 * Android-specific Ripple client.
 * 
 * @param TEvents Event type implementing [RippleEvent] for type-safe tracking
 * @param TMetadata Metadata type implementing [RippleMetadata] for type-safe metadata
 * @param config Ripple configuration
 */
class AndroidRippleClient<TEvents : RippleEvent, TMetadata : RippleMetadata>(
    private val appContext: Context?,
    config: RippleConfig
) : RippleClient<TEvents, TMetadata>(config) {

    constructor(config: RippleConfig) : this(null, config)

    private val platformInfo: Platform.Native by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Platform.Native(
            device = DeviceInfo(
                name = Build.MODEL ?: "unknown",
                version = Build.MANUFACTURER ?: "unknown"
            ),
            os = DeviceInfo(
                name = "Android",
                version = Build.VERSION.RELEASE ?: "unknown"
            )
        )
    }

    private val preferences by lazy(LazyThreadSafetyMode.PUBLICATION) {
        appContext?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private val application: Application?
        get() = appContext?.applicationContext as? Application

    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private var startedActivityCount: Int = 0
    private var lastAppState: AppState? = null

    override fun getPlatform(): Platform = platformInfo

    override fun init() {
        super.init()
        registerAppStateTracking()
    }

    override fun dispose() {
        unregisterAppStateTracking()
        super.dispose()
    }

    override fun loadAnonymousId(): String? = preferences?.getString(KEY_ANONYMOUS_ID, null)

    override fun saveAnonymousId(anonymousId: String) {
        preferences?.edit()?.putString(KEY_ANONYMOUS_ID, anonymousId)?.apply()
    }

    override fun loadUserId(): String? = preferences?.getString(KEY_USER_ID, null)

    override fun saveUserId(userId: String?) {
        val editor = preferences?.edit() ?: return
        if (userId == null) {
            editor.remove(KEY_USER_ID)
        } else {
            editor.putString(KEY_USER_ID, userId)
        }
        editor.apply()
    }

    fun screen(activity: Activity, payload: ScreenPayload? = null) {
        screen(
            ScreenPayload(
                title = payload?.title
                    ?: activity.title?.toString()?.takeIf { it.isNotBlank() }
                    ?: activity.localClassName,
                url = payload?.url ?: "android://${activity.packageName}/${activity.localClassName}",
                pathname = payload?.pathname ?: activity.localClassName,
                referrer = payload?.referrer,
                search = payload?.search,
                keywords = payload?.keywords,
                campaign = payload?.campaign,
                customProperties = payload?.customProperties
            )
        )
    }

    private fun registerAppStateTracking() {
        val application = application ?: return
        if (lifecycleCallbacks != null) return

        lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivityCount++
                if (startedActivityCount == 1) {
                    trackAppState(AppState.FOREGROUND)
                }
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                if (startedActivityCount == 0) {
                    trackAppState(AppState.BACKGROUND)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    private fun unregisterAppStateTracking() {
        val application = application ?: return
        lifecycleCallbacks?.let(application::unregisterActivityLifecycleCallbacks)
        lifecycleCallbacks = null
        startedActivityCount = 0
    }

    private fun trackAppState(newState: AppState) {
        if (lastAppState == newState) return

        events.appStateChanged(
            AppStateChangedPayload(
                newState = newState,
                previousState = lastAppState
            )
        )
        lastAppState = newState
    }

    companion object {
        private const val PREFERENCES_NAME = "ripple_identity"
        private const val KEY_ANONYMOUS_ID = "anonymous_id"
        private const val KEY_USER_ID = "user_id"

        /**
         * Create an untyped Android client for simple usage.
         */
        fun create(config: RippleConfig): AndroidRippleClient<DefaultRippleEvent, DefaultRippleMetadata> {
            return AndroidRippleClient(config)
        }

        fun create(
            context: Context,
            config: RippleConfig
        ): AndroidRippleClient<DefaultRippleEvent, DefaultRippleMetadata> {
            return AndroidRippleClient(context.applicationContext, config)
        }
    }
}
