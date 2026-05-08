package com.mian.accountrecord.tinker

import android.content.Context
import android.util.Log
import com.tencent.tinker.entry.ApplicationLike
import com.tencent.tinker.lib.tinker.TinkerInstaller

/**
 * Single entry point for Tinker hotfix lifecycle: install the loader and trigger patch downloads.
 */
object TinkerManager {

    private const val TAG = "TinkerManager"

    @Volatile
    private var installed: Boolean = false

    /**
     * Install Tinker. Must be called from [SampleApplicationLike.onBaseContextAttached] or as early
     * as possible in the host Application's attachBaseContext.
     */
    @JvmStatic
    fun installTinker(applicationLike: ApplicationLike) {
        if (installed) {
            Log.w(TAG, "Tinker already installed, skipping")
            return
        }
        try {
            TinkerInstaller.install(applicationLike)
            installed = true
            Log.i(TAG, "Tinker installed")
        } catch (t: Throwable) {
            Log.e(TAG, "Tinker install failed", t)
        }
    }

    /**
     * Apply a patch file from local storage. The path must point to a file the app can read,
     * typically inside [Context.getFilesDir]. Tinker copies the file into its own working dir,
     * verifies it, and triggers a process restart on the next launch.
     */
    @JvmStatic
    fun applyPatch(context: Context, patchFilePath: String) {
        if (!installed) {
            Log.w(TAG, "Tinker not installed; cannot apply patch")
            return
        }
        Log.i(TAG, "Applying patch from $patchFilePath")
        TinkerInstaller.onReceiveUpgradePatch(context.applicationContext, patchFilePath)
    }

    /** Clean any installed patch and revert to the base apk on next launch. */
    @JvmStatic
    fun cleanPatch(context: Context) {
        TinkerInstaller.cleanPatch(context.applicationContext)
    }
}
