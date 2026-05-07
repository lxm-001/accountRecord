package com.mian.accountrecord

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mian.accountrecord.data.local.db.AccountRecordDatabase
import com.mian.accountrecord.data.local.db.CategoryDao
import com.mian.accountrecord.data.local.db.LedgerDao
import com.mian.accountrecord.data.local.db.TransactionDao
import com.mian.accountrecord.data.local.entity.CategoryEntity
import com.mian.accountrecord.data.local.entity.LedgerEntity
import com.mian.accountrecord.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: AccountRecordDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var ledgerDao: LedgerDao

    private var ledgerId: Long = 0
    private var expenseCategoryId: Long = 0
    private var incomeCategoryId: Long = 0

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AccountRecordDatabase::class.java
        ).allowMainThreadQueries().build()

        transactionDao = db.transactionDao()
        categoryDao = db.categoryDao()
        ledgerDao = db.ledgerDao()

        // Insert prerequisite data for foreign keys
        expenseCategoryId = categoryDao.insert(
            CategoryEntity(name = "餐饮", icon = "restaurant", color = "#FF5722", type = 0)
        )
        incomeCategoryId = categoryDao.insert(
            CategoryEntity(name = "工资", icon = "wallet", color = "#4CAF50", type = 1)
        )
        ledgerId = ledgerDao.insert(
            LedgerEntity(name = "日常账本", icon = "home", template = "daily", isActive = true)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_query_transaction() = runTest {
        val tx = TransactionEntity(
            amount = 2500, type = 0, categoryId = expenseCategoryId,
            ledgerId = ledgerId, date = 1000L
        )
        val id = transactionDao.insert(tx)
        assertTrue(id > 0)

        val result = transactionDao.getByLedgerAndDateRange(ledgerId, 0L, 2000L).first()
        assertEquals(1, result.size)
        assertEquals(2500L, result[0].amount)
    }

    @Test
    fun update_transaction() = runTest {
        val tx = TransactionEntity(
            amount = 1000, type = 0, categoryId = expenseCategoryId,
            ledgerId = ledgerId, date = 1000L
        )
        val id = transactionDao.insert(tx)

        val updated = tx.copy(id = id, amount = 2000)
        transactionDao.update(updated)

        val result = transactionDao.getByLedgerAndDateRange(ledgerId, 0L, 2000L).first()
        assertEquals(1, result.size)
        assertEquals(2000L, result[0].amount)
    }

    @Test
    fun delete_transaction() = runTest {
        val tx = TransactionEntity(
            amount = 500, type = 0, categoryId = expenseCategoryId,
            ledgerId = ledgerId, date = 1000L
        )
        val id = transactionDao.insert(tx)
        transactionDao.delete(tx.copy(id = id))

        val result = transactionDao.getByLedgerAndDateRange(ledgerId, 0L, 2000L).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun insertAll_batch_insert() = runTest {
        val txList = listOf(
            TransactionEntity(amount = 100, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 1000L),
            TransactionEntity(amount = 200, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 1500L)
        )
        val ids = transactionDao.insertAll(txList)
        assertEquals(2, ids.size)

        val result = transactionDao.getByLedgerAndDateRange(ledgerId, 0L, 2000L).first()
        assertEquals(2, result.size)
    }

    @Test
    fun getByLedgerAndDateRange_filters_correctly() = runTest {
        transactionDao.insert(TransactionEntity(amount = 100, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 500L))
        transactionDao.insert(TransactionEntity(amount = 200, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 1500L))
        transactionDao.insert(TransactionEntity(amount = 300, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 2500L))

        // Only date 1500 should be in range [1000, 2000)
        val result = transactionDao.getByLedgerAndDateRange(ledgerId, 1000L, 2000L).first()
        assertEquals(1, result.size)
        assertEquals(200L, result[0].amount)
    }

    @Test
    fun getByLedgerAndDateRange_orders_by_date_desc() = runTest {
        transactionDao.insert(TransactionEntity(amount = 100, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 1000L))
        transactionDao.insert(TransactionEntity(amount = 300, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 3000L))
        transactionDao.insert(TransactionEntity(amount = 200, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 2000L))

        val result = transactionDao.getByLedgerAndDateRange(ledgerId, 0L, 5000L).first()
        assertEquals(3, result.size)
        assertEquals(3000L, result[0].date)
        assertEquals(2000L, result[1].date)
        assertEquals(1000L, result[2].date)
    }

    @Test
    fun getSummaryByLedgerAndDateRange_groups_by_type() = runTest {
        // 2 expenses, 1 income
        transactionDao.insert(TransactionEntity(amount = 1000, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 1000L))
        transactionDao.insert(TransactionEntity(amount = 2000, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 1500L))
        transactionDao.insert(TransactionEntity(amount = 5000, type = 1, categoryId = incomeCategoryId, ledgerId = ledgerId, date = 1200L))

        val summary = transactionDao.getSummaryByLedgerAndDateRange(ledgerId, 0L, 3000L).first()
        assertEquals(2, summary.size)

        val expenseTotal = summary.find { it.type == 0 }?.total ?: 0L
        val incomeTotal = summary.find { it.type == 1 }?.total ?: 0L
        assertEquals(3000L, expenseTotal)
        assertEquals(5000L, incomeTotal)
    }

    @Test
    fun getSummaryByLedgerAndDateRange_empty_when_no_data() = runTest {
        val summary = transactionDao.getSummaryByLedgerAndDateRange(ledgerId, 0L, 3000L).first()
        assertTrue(summary.isEmpty())
    }

    @Test
    fun getExpenseByCategoryAndDateRange_only_expenses() = runTest {
        val cat2Id = categoryDao.insert(
            CategoryEntity(name = "交通", icon = "bus", color = "#2196F3", type = 0)
        )
        transactionDao.insert(TransactionEntity(amount = 1000, type = 0, categoryId = expenseCategoryId, ledgerId = ledgerId, date = 1000L))
        transactionDao.insert(TransactionEntity(amount = 3000, type = 0, categoryId = cat2Id, ledgerId = ledgerId, date = 1500L))
        // Income should be excluded
        transactionDao.insert(TransactionEntity(amount = 9000, type = 1, categoryId = incomeCategoryId, ledgerId = ledgerId, date = 1200L))

        val result = transactionDao.getExpenseByCategoryAndDateRange(ledgerId, 0L, 3000L).first()
        assertEquals(2, result.size)
        // Ordered by total DESC
        assertEquals(cat2Id, result[0].categoryId)
        assertEquals(3000L, result[0].total)
        assertEquals(expenseCategoryId, result[1].categoryId)
        assertEquals(1000L, result[1].total)
    }

    @Test
    fun getExpenseByCategoryAndDateRange_empty_when_no_expenses() = runTest {
        // Only income
        transactionDao.insert(TransactionEntity(amount = 5000, type = 1, categoryId = incomeCategoryId, ledgerId = ledgerId, date = 1000L))

        val result = transactionDao.getExpenseByCategoryAndDateRange(ledgerId, 0L, 3000L).first()
        assertTrue(result.isEmpty())
    }
}
