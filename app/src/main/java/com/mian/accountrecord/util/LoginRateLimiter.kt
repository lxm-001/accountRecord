package com.mian.accountrecord.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 登录频率限制器。
 * 记录 1 分钟内的登录尝试次数，超过 5 次触发 3 分钟冷却期。
 */
@Singleton
class LoginRateLimiter @Inject constructor() {

    private val loginTimestamps = mutableListOf<Long>()
    private var cooldownUntil: Long = 0L

    fun canLogin(): Boolean {
        if (System.currentTimeMillis() < cooldownUntil) return false
        return true
    }

    fun recordLoginAttempt() {
        val now = System.currentTimeMillis()
        loginTimestamps.add(now)
        loginTimestamps.removeAll { now - it > 60_000 }
        if (loginTimestamps.size > 5) {
            cooldownUntil = now + 3 * 60_000
        }
    }

    fun getCooldownRemainingSeconds(): Int {
        val remaining = cooldownUntil - System.currentTimeMillis()
        return if (remaining > 0) (remaining / 1000).toInt() else 0
    }
}
