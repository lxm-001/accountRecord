package com.mian.accountrecord

import android.content.Context
import android.os.SystemClock
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.mian.accountrecord.tinker.SampleApplicationLike
import com.mian.accountrecord.util.NotificationHelper
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.tencent.mmkv.MMKV
import com.tencent.tinker.loader.shareutil.ShareConstants
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AccountRecordApp : MultiDexApplication() {

    companion object {
        const val WECHAT_APP_ID = "YOUR_WECHAT_APP_ID"

        lateinit var wxApi: IWXAPI
            private set
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(base)

        // Install Tinker BEFORE any other framework that touches our patched classes.
        // Hilt's generated component is created lazily on first injection, so it stays cold here.
        val applicationLike = SampleApplicationLike(
            this,
            ShareConstants.TINKER_ENABLE_ALL,
            true,
            SystemClock.elapsedRealtime(),
            System.currentTimeMillis(),
            null
        )
        applicationLike.onBaseContextAttached(base)
    }

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        NotificationHelper.createChannel(this)

        // Register WeChat SDK
        wxApi = WXAPIFactory.createWXAPI(this, WECHAT_APP_ID, true)
        wxApi.registerApp(WECHAT_APP_ID)
    }
}
