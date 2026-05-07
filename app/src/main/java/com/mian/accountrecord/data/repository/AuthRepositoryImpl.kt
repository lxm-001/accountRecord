package com.mian.accountrecord.data.repository

import com.mian.accountrecord.data.local.db.UserDao
import com.mian.accountrecord.data.local.entity.UserEntity
import com.mian.accountrecord.data.local.preferences.AuthPreferences
import com.mian.accountrecord.domain.model.OAuthProvider
import com.mian.accountrecord.domain.model.User
import com.mian.accountrecord.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authPreferences: AuthPreferences,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun saveLoginInfo(user: User): Result<Unit> = runCatching {
        authPreferences.saveLoginInfo(
            openId = user.openId,
            nickname = user.nickname,
            avatarUrl = user.avatarUrl,
            provider = user.oauthProvider.name.lowercase()
        )
    }

    override suspend fun clearAuth(): Result<Unit> = runCatching {
        authPreferences.clearAll()
    }

    override suspend fun getCurrentUser(): User? {
        if (!authPreferences.isLoginInfoValid()) return null
        val openId = authPreferences.currentOpenId ?: return null
        val nickname = authPreferences.currentNickname ?: return null
        val provider = when (authPreferences.currentProvider) {
            "wechat" -> OAuthProvider.WECHAT
            "alipay" -> OAuthProvider.ALIPAY
            else -> return null
        }
        return User(openId, nickname, authPreferences.currentAvatarUrl, provider)
    }

    override suspend fun hasLoginHistory(openId: String): Boolean {
        return userDao.hasLoginHistory(openId) > 0
    }

    override suspend fun recordLogin(user: User) {
        userDao.upsert(
            UserEntity(
                openId = user.openId,
                nickname = user.nickname,
                avatarUrl = user.avatarUrl,
                oauthProvider = user.oauthProvider.name.lowercase(),
                lastLoginAt = System.currentTimeMillis()
            )
        )
    }
}
