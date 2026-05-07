package com.mian.accountrecord.domain.repository

import com.mian.accountrecord.domain.model.Ledger
import kotlinx.coroutines.flow.Flow

interface LedgerRepository {

    fun getAll(): Flow<List<Ledger>>

    fun getActive(): Flow<Ledger?>

    suspend fun insert(ledger: Ledger): Long

    suspend fun update(ledger: Ledger)

    suspend fun delete(ledger: Ledger)

    suspend fun switchTo(id: Long)

    suspend fun count(): Int
}
