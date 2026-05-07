package com.mian.accountrecord.data.local.preferences

import com.tencent.mmkv.MMKV
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor() {

    private val mmkv: MMKV = MMKV.defaultMMKV()

    var lastSelectedCategoryId: Long
        get() = mmkv.decodeLong(KEY_LAST_SELECTED_CATEGORY_ID, DEFAULT_ID)
        set(value) { mmkv.encode(KEY_LAST_SELECTED_CATEGORY_ID, value) }

    var activeLedgerId: Long
        get() = mmkv.decodeLong(KEY_ACTIVE_LEDGER_ID, DEFAULT_ID)
        set(value) { mmkv.encode(KEY_ACTIVE_LEDGER_ID, value) }

    companion object {
        private const val KEY_LAST_SELECTED_CATEGORY_ID = "last_selected_category_id"
        private const val KEY_ACTIVE_LEDGER_ID = "active_ledger_id"
        private const val DEFAULT_ID = -1L
    }
}
