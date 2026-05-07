package com.mian.accountrecord.data.local.preferences

import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthPreferences @Inject constructor() {

    private val mmkv: MMKV = MMKV.mmkvWithID("auth_prefs")

    var isLoggedIn: Boolean
        get() = mmkv.decodeBool(KEY_IS_LOGGED_IN, false)
        set(value) { mmkv.encode(KEY_IS_LOGGED_IN, value) }

    var currentOpenId: String?
        get() = mmkv.decodeString(KEY_CURRENT_OPEN_ID, null)
        set(value) { mmkv.encode(KEY_CURRENT_OPEN_ID, value ?: "") }

    var currentNickname: String?
        get() = mmkv.decodeString(KEY_CURRENT_NICKNAME, null)
        set(value) { mmkv.encode(KEY_CURRENT_NICKNAME, value ?: "") }

    var currentAvatarUrl: String?
        get() = mmkv.decodeString(KEY_CURRENT_AVATAR_URL, null)
        set(value) { mmkv.encode(KEY_CURRENT_AVATAR_URL, value ?: "") }

    var currentProvider: String?
        get() = mmkv.decodeString(KEY_CURRENT_PROVIDER, null)
        set(value) { mmkv.encode(KEY_CURRENT_PROVIDER, value ?: "") }

    var currentPhone: String?
        get() = mmkv.decodeString(KEY_CURRENT_PHONE, null)
        set(value) { mmkv.encode(KEY_CURRENT_PHONE, value ?: "") }

    var currentEmail: String?
        get() = mmkv.decodeString(KEY_CURRENT_EMAIL, null)
        set(value) { mmkv.encode(KEY_CURRENT_EMAIL, value ?: "") }

    fun saveLoginInfo(openId: String, nickname: String, avatarUrl: String?, provider: String) {
        isLoggedIn = true
        currentOpenId = openId
        currentNickname = nickname
        currentAvatarUrl = avatarUrl
        currentProvider = provider
    }

    fun clearAll() {
        mmkv.clearAll()
    }

    fun isLoginInfoValid(): Boolean {
        return isLoggedIn && !currentOpenId.isNullOrEmpty() && !currentNickname.isNullOrEmpty()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_OPEN_ID = "current_open_id"
        private const val KEY_CURRENT_NICKNAME = "current_nickname"
        private const val KEY_CURRENT_AVATAR_URL = "current_avatar_url"
        private const val KEY_CURRENT_PROVIDER = "current_provider"
        private const val KEY_CURRENT_PHONE = "current_phone"
        private const val KEY_CURRENT_EMAIL = "current_email"
    }
}
