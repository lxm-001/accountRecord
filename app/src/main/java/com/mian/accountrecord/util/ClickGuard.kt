package com.mian.accountrecord.util

/**
 * 按钮防重复点击控制器。
 * 点击后 3 秒内禁用重复点击，防止用户短时间内多次触发登录请求。
 */
class ClickGuard {
    private var lastClickTime = 0L
    private val interval = 3000L // 3 秒

    fun isClickAllowed(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickTime < interval) return false
        lastClickTime = now
        return true
    }

    fun reset() {
        lastClickTime = 0L
    }
}
