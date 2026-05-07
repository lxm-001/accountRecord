package com.mian.accountrecord.domain.repository

import com.mian.accountrecord.domain.model.User

interface AuthRepository {
    suspend fun saveLoginInfo(user: User): Result<Unit>
    suspend fun clearAuth(): Result<Unit>
    suspend fun getCurrentUser(): User?
    suspend fun hasLoginHistory(openId: String): Boolean
    suspend fun recordLogin(user: User)
}
