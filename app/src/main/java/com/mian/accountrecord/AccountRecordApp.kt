package com.mian.accountrecord

import android.app.Application
import com.mian.accountrecord.util.NotificationHelper
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AccountRecordApp : Application() {

    companion object {
        const val WECHAT_APP_ID = "YOUR_WECHAT_APP_ID"

        lateinit var wxApi: IWXAPI
            private set
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
