package com.mian.accountrecord.domain.model

data class LoginResult(
    val user: User,
    val isFirstLogin: Boolean
)
