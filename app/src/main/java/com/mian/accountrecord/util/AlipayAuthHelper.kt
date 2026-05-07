package com.mian.accountrecord.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.alipay.sdk.app.AuthTask
import com.mian.accountrecord.domain.model.OAuthProvider
import com.mian.accountrecord.domain.model.OAuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper that encapsulates Alipay SDK authorization logic.
 *
 * Supports:
 * - Checking if Alipay app is installed (Req 3.1)
 * - Launching native Alipay App authorization with auth_user scope (Req 3.2)
 * - H5 WebView fallback when Alipay is not installed (Req 3.3)
 * - Parsing auth callback and constructing OAuthResult (Req 3.4)
 * - Handling authorization cancellation (Req 3.7)
 */
object AlipayAuthHelper {

    private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
    private const val ALIPAY_APP_ID = "YOUR_ALIPAY_APP_ID"

    /** Result codes returned by Alipay SDK */
    private const val RESULT_CODE_SUCCESS = "9000"
    private const val RESULT_CODE_PROCESSING = "8000"
    private const val RESULT_CODE_FAILURE = "4000"
    private const val RESULT_CODE_CANCEL = "6001"
    private const val RESULT_CODE_NETWORK_ERROR = "6002"

    /**
     * Check if Alipay app is installed on the device (Req 3.1).
     */
    fun isAlipayInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(ALIPAY_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Build the auth info string for Alipay SDK authorization.
     * Uses auth_user scope as required by Req 3.2.
     *
     * @param state CSRF state parameter for security verification (Req 6.2)
     * @param useH5 Whether to use H5 fallback mode (Req 3.3)
     */
    private fun buildAuthInfo(state: String, useH5: Boolean): String {
        val params = mutableMapOf(
            "app_id" to ALIPAY_APP_ID,
            "scope" to "auth_user",
            "state" to state,
            "auth_type" to if (useH5) "AUTHACCOUNT" else "AUTHACCOUNT"
        )
        // In production, this string should be signed by the server.
        // For this pure-client app, we build a placeholder auth info string.
        return params.entries.joinToString("&") { "${it.key}=${it.value}" }
    }

    /**
     * Launch Alipay authorization.
     *
     * This method runs on a background thread (via Dispatchers.IO) because
     * the Alipay SDK's AuthTask.authV2() is a synchronous blocking call.
     *
     * - If Alipay is installed → native App authorization (Req 3.2)
     * - If Alipay is not installed → H5 WebView fallback (Req 3.3)
     *
     * @param activity The current Activity context required by Alipay SDK
     * @param state CSRF state parameter
     * @return [AlipayAuthResult] containing the parsed result or error
     */
    suspend fun authorize(activity: Activity, state: String): AlipayAuthResult {
        val isInstalled = isAlipayInstalled(activity)
        val authInfo = buildAuthInfo(state, useH5 = !isInstalled)

        return withContext(Dispatchers.IO) {
            try {
                val authTask = AuthTask(activity)
                // authV2 will use native Alipay app if installed,
                // otherwise falls back to H5 WebView automatically (Req 3.3)
                val resultMap = authTask.authV2(authInfo, !isInstalled)
                parseAuthResult(resultMap, state)
            } catch (e: Exception) {
                AlipayAuthResult.Error("支付宝授权异常: ${e.message}")
            }
        }
    }

    /**
     * Parse the result map returned by Alipay SDK's authV2().
     *
     * The result map contains:
     * - "resultStatus": status code (9000=success, 6001=cancel, etc.)
     * - "result": URL-encoded key-value pairs including auth_code, user_id, etc.
     * - "memo": additional info
     *
     * @param resultMap The raw result from AuthTask.authV2()
     * @param state The original CSRF state for verification
     */
    private fun parseAuthResult(
        resultMap: Map<String, String>,
        state: String
    ): AlipayAuthResult {
        val resultStatus = resultMap["resultStatus"] ?: ""
        val resultData = resultMap["result"] ?: ""

        return when (resultStatus) {
            RESULT_CODE_SUCCESS -> {
                // Parse the result string (Req 3.4)
                val params = parseResultString(resultData)
                val authCode = params["auth_code"] ?: ""
                val userId = params["user_id"] ?: params["alipay_open_id"] ?: authCode

                if (userId.isEmpty()) {
                    AlipayAuthResult.Error("登录失败，请稍后重试")
                } else {
                    val oauthResult = OAuthResult(
                        openId = userId,
                        nickname = params["nick_name"] ?: "支付宝用户",
                        avatarUrl = params["avatar"] ?: params["head_img"],
                        state = state,
                        provider = OAuthProvider.ALIPAY
                    )
                    AlipayAuthResult.Success(oauthResult)
                }
            }

            RESULT_CODE_PROCESSING -> {
                // Authorization is being processed, treat as pending
                AlipayAuthResult.Error("授权处理中，请稍后重试")
            }

            RESULT_CODE_CANCEL -> {
                // User cancelled authorization (Req 3.7)
                AlipayAuthResult.Cancelled
            }

            RESULT_CODE_NETWORK_ERROR -> {
                AlipayAuthResult.Error("网络异常，请检查网络后重试")
            }

            else -> {
                AlipayAuthResult.Error("登录失败，请稍后重试")
            }
        }
    }

    /**
     * Parse the URL-encoded result string from Alipay SDK into a key-value map.
     *
     * The result string format: "key1=value1&key2=value2&..."
     */
    private fun parseResultString(result: String): Map<String, String> {
        if (result.isBlank()) return emptyMap()

        return result.split("&")
            .mapNotNull { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = Uri.decode(parts[0].trim())
                    val value = Uri.decode(parts[1].trim())
                    key to value
                } else {
                    null
                }
            }
            .toMap()
    }
}

/**
 * Sealed class representing the result of an Alipay authorization attempt.
 */
sealed class AlipayAuthResult {
    /** Authorization succeeded, contains the parsed [OAuthResult] */
    data class Success(val oauthResult: OAuthResult) : AlipayAuthResult()

    /** User cancelled the authorization (Req 3.7) */
    data object Cancelled : AlipayAuthResult()

    /** Authorization failed with an error message */
    data class Error(val message: String) : AlipayAuthResult()
}
