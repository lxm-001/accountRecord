package com.mian.accountrecord.tinker

import android.app.Application
import android.content.Context
import android.content.Intent
import com.tencent.tinker.entry.DefaultApplicationLike

/**
 * ApplicationLike wrapper required by Tinker. We do NOT subclass TinkerApplication directly,
 * so that the host Application can keep `@HiltAndroidApp`. Hilt rewrites the Application's
 * superclass at compile time, which conflicts with TinkerApplication's parameterised constructor.
 *
 * Instead, the host Application creates this Like in attachBaseContext and hands it to
 * [TinkerManager.installTinker]. All actual lifecycle work stays in [com.mian.accountrecord.AccountRecordApp].
 */
class SampleApplicationLike(
    application: Application,
    tinkerFlags: Int,
    tinkerLoadVerifyFlag: Boolean,
    applicationStartElapsedTime: Long,
    applicationStartMillisTime: Long,
    tinkerResultIntent: Intent?
) : DefaultApplicationLike(
    application,
    tinkerFlags,
    tinkerLoadVerifyFlag,
    applicationStartElapsedTime,
    applicationStartMillisTime,
    tinkerResultIntent
) {

    override fun onBaseContextAttached(base: Context) {
        super.onBaseContextAttached(base)
        // Multidex is already installed by the host Application.
        // Install Tinker as the very first thing so patched classes win against the base apk.
        TinkerManager.installTinker(this)
    }
}
