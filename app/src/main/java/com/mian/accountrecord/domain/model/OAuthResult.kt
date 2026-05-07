package com.mian.accountrecord.domain.model

data class OAuthResult(
    val openId: String,
    val nickname: String,
    val avatarUrl: String?,
    val state: String,
    val provider: OAuthProvider
)
