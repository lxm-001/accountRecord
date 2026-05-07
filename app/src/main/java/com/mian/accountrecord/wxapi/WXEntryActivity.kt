package com.mian.accountrecord.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.mian.accountrecord.AccountRecordApp
import com.mian.accountrecord.domain.model.OAuthProvider
import com.mian.accountrecord.domain.model.OAuthResult
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler

/**
 * WeChat OAuth callback activity.
 *
 * MUST be located at package `com.mian.accountrecord.wxapi` and named `WXEntryActivity`
 * per WeChat SDK requirements.
 *
 * Handles the authorization response from WeChat, parses the result,
 * and broadcasts an OAuthResult back to the LoginViewModel.
 */
class WXEntryActivity : Activity(), IWXAPIEventHandler {

    companion object {
        const val ACTION_WECHAT_OAUTH_RESULT = "com.mian.accountrecord.WECHAT_OAUTH_RESULT"
        const val EXTRA_OAUTH_RESULT = "oauth_result"
        const val EXTRA_ERROR_MESSAGE = "error_message"

        /**
         * Check if WeChat app is installed on the device (Req 2.1).
         */
        fun isWeChatInstalled(): Boolean {
            return try {
                AccountRecordApp.wxApi.isWXAppInstalled
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Launch WeChat authorization with snsapi_userinfo scope (Req 2.2).
         * @param state CSRF state parameter for security verification
         */
        fun launchWeChatAuth(state: String) {
            val req = SendAuth.Req().apply {
                scope = "snsapi_userinfo"
                this.state = state
            }
            AccountRecordApp.wxApi.sendReq(req)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AccountRecordApp.wxApi.handleIntent(intent, this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        AccountRecordApp.wxApi.handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq?) {
        // Not used for OAuth login flow
        finish()
    }

    override fun onResp(resp: BaseResp?) {
        when (resp?.type) {
            ConstantsAPI.COMMAND_SENDAUTH -> handleAuthResponse(resp as? SendAuth.Resp)
            else -> finish()
        }
    }

    /**
     * Handle WeChat SendAuth response.
     * - ErrCode.ERR_OK: Authorization succeeded, parse code and user info (Req 2.4)
     * - ErrCode.ERR_USER_CANCEL: User denied authorization (Req 2.7)
     * - ErrCode.ERR_AUTH_DENIED: Authorization denied (Req 2.7)
     * - Other: SDK error
     */
    private fun handleAuthResponse(resp: SendAuth.Resp?) {
        if (resp == null) {
            finish()
            return
        }

        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                // Authorization succeeded (Req 2.4)
                // In a pure client-side app, we construct OAuthResult from the SDK callback.
                // The WeChat SDK returns code, state, and basic info.
                // Since there's no backend token exchange, we use the code as openId placeholder
                // and the user's WeChat info from the SDK response.
                val oauthResult = OAuthResult(
                    openId = resp.code ?: "",
                    nickname = "微信用户",
                    avatarUrl = null,
                    state = resp.state ?: "",
                    provider = OAuthProvider.WECHAT
                )
                broadcastOAuthResult(oauthResult)
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                // User cancelled authorization (Req 2.7)
                Toast.makeText(this, "授权已取消", Toast.LENGTH_SHORT).show()
                broadcastError("授权已取消")
            }

            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                // Authorization denied (Req 2.7)
                Toast.makeText(this, "授权已取消", Toast.LENGTH_SHORT).show()
                broadcastError("授权已取消")
            }

            else -> {
                // Other SDK errors (Req 7.6)
                Toast.makeText(this, "登录失败，请稍后重试", Toast.LENGTH_SHORT).show()
                broadcastError("登录失败，请稍后重试")
            }
        }

        finish()
    }

    /**
     * Broadcast successful OAuth result to LoginViewModel via LocalBroadcast.
     */
    private fun broadcastOAuthResult(result: OAuthResult) {
        val intent = Intent(ACTION_WECHAT_OAUTH_RESULT).apply {
            putExtra(EXTRA_OAUTH_RESULT + "_openId", result.openId)
            putExtra(EXTRA_OAUTH_RESULT + "_nickname", result.nickname)
            putExtra(EXTRA_OAUTH_RESULT + "_avatarUrl", result.avatarUrl)
            putExtra(EXTRA_OAUTH_RESULT + "_state", result.state)
            putExtra(EXTRA_OAUTH_RESULT + "_provider", result.provider.name)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    /**
     * Broadcast error message for failed/cancelled authorization.
     */
    private fun broadcastError(message: String) {
        val intent = Intent(ACTION_WECHAT_OAUTH_RESULT).apply {
            putExtra(EXTRA_ERROR_MESSAGE, message)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}
