package com.mian.accountrecord.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mian.accountrecord.data.local.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Query("SELECT * FROM ledgers ORDER BY created_at ASC")
    fun getAll(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledgers WHERE is_active = 1 LIMIT 1")
    fun getActive(): Flow<LedgerEntity?>

    @Insert
    suspend fun insert(ledger: LedgerEntity): Long

    @Update
    suspend fun update(ledger: LedgerEntity)

    @Delete
    suspend fun delete(ledger: LedgerEntity)

    @Query("UPDATE ledgers SET is_active = 0")
    suspend fun deactivateAll()

    @Query("UPDATE ledgers SET is_active = 1 WHERE id = :id")
    suspend fun activate(id: Long)

    @Query("SELECT COUNT(*) FROM ledgers")
    suspend fun count(): Int
}
