package com.mian.accountrecord.data.repository

import com.mian.accountrecord.data.local.db.LedgerDao
import com.mian.accountrecord.data.mapper.toDomain
import com.mian.accountrecord.data.mapper.toEntity
import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.repository.LedgerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LedgerRepositoryImpl @Inject constructor(
    private val ledgerDao: LedgerDao
) : LedgerRepository {

    override fun getAll(): Flow<List<Ledger>> {
        return ledgerDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActive(): Flow<Ledger?> {
        return ledgerDao.getActive().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun insert(ledger: Ledger): Long {
        return ledgerDao.insert(ledger.toEntity())
    }

    override suspend fun update(ledger: Ledger) {
        ledgerDao.update(ledger.toEntity())
    }

    override suspend fun delete(ledger: Ledger) {
        ledgerDao.delete(ledger.toEntity())
    }

    override suspend fun switchTo(id: Long) {
        ledgerDao.deactivateAll()
        ledgerDao.activate(id)
    }

    override suspend fun count(): Int {
        return ledgerDao.count()
    }
}
