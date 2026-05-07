package com.mian.accountrecord.domain.model

data class User(
    val openId: String,
    val nickname: String,
    val avatarUrl: String?,
    val oauthProvider: OAuthProvider
)
