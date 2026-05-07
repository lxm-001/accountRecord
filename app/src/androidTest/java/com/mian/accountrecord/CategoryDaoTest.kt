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
class CategoryDaoTest {

    private lateinit var db: AccountRecordDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var ledgerDao: LedgerDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AccountRecordDatabase::class.java
        ).allowMainThreadQueries().build()

        categoryDao = db.categoryDao()
        transactionDao = db.transactionDao()
        ledgerDao = db.ledgerDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_getAll() = runTest {
        categoryDao.insert(CategoryEntity(name = "餐饮", icon = "restaurant", color = "#FF5722", type = 0, sortOrder = 1))
        categoryDao.insert(CategoryEntity(name = "交通", icon = "bus", color = "#2196F3", type = 0, sortOrder = 0))

        val all = categoryDao.getAll().first()
        assertEquals(2, all.size)
        // Ordered by sort_order ASC
        assertEquals("交通", all[0].name)
        assertEquals("餐饮", all[1].name)
    }

    @Test
    fun getByType_filters_correctly() = runTest {
        categoryDao.insert(CategoryEntity(name = "餐饮", icon = "restaurant", color = "#FF5722", type = 0, sortOrder = 0))
        categoryDao.insert(CategoryEntity(name = "工资", icon = "wallet", color = "#4CAF50", type = 1, sortOrder = 0))
        categoryDao.insert(CategoryEntity(name = "交通", icon = "bus", color = "#2196F3", type = 0, sortOrder = 1))

        val expenses = categoryDao.getByType(0).first()
        assertEquals(2, expenses.size)

        val incomes = categoryDao.getByType(1).first()
        assertEquals(1, incomes.size)
        assertEquals("工资", incomes[0].name)
    }

    @Test
    fun update_category() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "餐饮", icon = "restaurant", color = "#FF5722", type = 0))
        categoryDao.update(CategoryEntity(id = id, name = "美食", icon = "restaurant", color = "#FF5722", type = 0))

        val all = categoryDao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("美食", all[0].name)
    }

    @Test
    fun delete_category() = runTest {
        val id = categoryDao.insert(CategoryEntity(name = "餐饮", icon = "restaurant", color = "#FF5722", type = 0))
        categoryDao.delete(CategoryEntity(id = id, name = "餐饮", icon = "restaurant", color = "#FF5722", type = 0))

        val all = categoryDao.getAll().first()
        assertTrue(all.isEmpty())
    }

    @Test
    fun updateSortOrder_changes_order() = runTest {
        val id1 = categoryDao.insert(CategoryEntity(name = "餐饮", icon = "restaurant", color = "#FF5722", type = 0, sortOrder = 0))
        val id2 = categoryDao.insert(CategoryEntity(name = "交通", icon = "bus", color = "#2196F3", type = 0, sortOrder = 1))

        // Swap sort orders
        categoryDao.updateSortOrder(id1, 1)
        categoryDao.updateSortOrder(id2, 0)

        val all = categoryDao.getAll().first()
        assertEquals("交通", all[0].name)
        assertEquals("餐饮", all[1].name)
    }

    @Test
    fun migrateTransactions_moves_records_to_target_category() = runTest {
        val sourceId = categoryDao.insert(CategoryEntity(name = "旧分类", icon = "old", color = "#000000", type = 0))
        val targetId = categoryDao.insert(CategoryEntity(name = "其他", icon = "more", color = "#607D8B", type = 0))
        val ledgerId = ledgerDao.insert(LedgerEntity(name = "测试账本", icon = "home", isActive = true))

        // Insert transactions under source category
        transactionDao.insert(TransactionEntity(amount = 100, type = 0, categoryId = sourceId, ledgerId = ledgerId, date = 1000L))
        transactionDao.insert(TransactionEntity(amount = 200, type = 0, categoryId = sourceId, ledgerId = ledgerId, date = 2000L))
        transactionDao.insert(TransactionEntity(amount = 300, type = 0, categoryId = targetId, ledgerId = ledgerId, date = 3000L))

        // Migrate source → target
        categoryDao.migrateTransactions(sourceId, targetId)

        val all = transactionDao.getByLedgerAndDateRange(ledgerId, 0L, 5000L).first()
        assertEquals(3, all.size)
        // All transactions should now belong to targetId
        assertTrue(all.all { it.categoryId == targetId })
    }

    @Test
    fun migrateTransactions_no_effect_when_no_matching_records() = runTest {
        val sourceId = categoryDao.insert(CategoryEntity(name = "旧分类", icon = "old", color = "#000000", type = 0))
        val targetId = categoryDao.insert(CategoryEntity(name = "其他", icon = "more", color = "#607D8B", type = 0))
        val ledgerId = ledgerDao.insert(LedgerEntity(name = "测试账本", icon = "home", isActive = true))

        transactionDao.insert(TransactionEntity(amount = 100, type = 0, categoryId = targetId, ledgerId = ledgerId, date = 1000L))

        // Migrate from source that has no transactions
        categoryDao.migrateTransactions(sourceId, targetId)

        val all = transactionDao.getByLedgerAndDateRange(ledgerId, 0L, 5000L).first()
        assertEquals(1, all.size)
        assertEquals(targetId, all[0].categoryId)
    }

    @Test
    fun getByType_ordered_by_sortOrder_asc() = runTest {
        categoryDao.insert(CategoryEntity(name = "C", icon = "c", color = "#000", type = 0, sortOrder = 2))
        categoryDao.insert(CategoryEntity(name = "A", icon = "a", color = "#000", type = 0, sortOrder = 0))
        categoryDao.insert(CategoryEntity(name = "B", icon = "b", color = "#000", type = 0, sortOrder = 1))

        val result = categoryDao.getByType(0).first()
        assertEquals("A", result[0].name)
        assertEquals("B", result[1].name)
        assertEquals("C", result[2].name)
    }
}
