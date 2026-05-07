package com.mian.accountrecord

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mian.accountrecord.data.local.db.AccountRecordDatabase
import com.mian.accountrecord.data.local.db.LedgerDao
import com.mian.accountrecord.data.local.entity.LedgerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerDaoTest {

    private lateinit var db: AccountRecordDatabase
    private lateinit var ledgerDao: LedgerDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AccountRecordDatabase::class.java
        ).allowMainThreadQueries().build()

        ledgerDao = db.ledgerDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_getAll() = runTest {
        ledgerDao.insert(LedgerEntity(name = "日常账本", icon = "home", template = "daily", isActive = true))
        ledgerDao.insert(LedgerEntity(name = "旅行账本", icon = "flight", template = "travel", isActive = false))

        val all = ledgerDao.getAll().first()
        assertEquals(2, all.size)
    }

    @Test
    fun getAll_ordered_by_createdAt_asc() = runTest {
        ledgerDao.insert(LedgerEntity(name = "第二个", icon = "b", createdAt = 2000L))
        ledgerDao.insert(LedgerEntity(name = "第一个", icon = "a", createdAt = 1000L))
        ledgerDao.insert(LedgerEntity(name = "第三个", icon = "c", createdAt = 3000L))

        val all = ledgerDao.getAll().first()
        assertEquals("第一个", all[0].name)
        assertEquals("第二个", all[1].name)
        assertEquals("第三个", all[2].name)
    }

    @Test
    fun getActive_returns_active_ledger() = runTest {
        ledgerDao.insert(LedgerEntity(name = "非活跃", icon = "a", isActive = false))
        ledgerDao.insert(LedgerEntity(name = "活跃", icon = "b", isActive = true))

        val active = ledgerDao.getActive().first()
        assertNotNull(active)
        assertEquals("活跃", active!!.name)
    }

    @Test
    fun getActive_returns_null_when_none_active() = runTest {
        ledgerDao.insert(LedgerEntity(name = "非活跃", icon = "a", isActive = false))

        val active = ledgerDao.getActive().first()
        assertNull(active)
    }

    @Test
    fun deactivateAll_sets_all_inactive() = runTest {
        ledgerDao.insert(LedgerEntity(name = "A", icon = "a", isActive = true))
        ledgerDao.insert(LedgerEntity(name = "B", icon = "b", isActive = true))

        ledgerDao.deactivateAll()

        val all = ledgerDao.getAll().first()
        assertTrue(all.all { !it.isActive })
        assertNull(ledgerDao.getActive().first())
    }

    @Test
    fun activate_sets_specific_ledger_active() = runTest {
        val id1 = ledgerDao.insert(LedgerEntity(name = "A", icon = "a", isActive = false))
        val id2 = ledgerDao.insert(LedgerEntity(name = "B", icon = "b", isActive = false))

        ledgerDao.activate(id2)

        val active = ledgerDao.getActive().first()
        assertNotNull(active)
        assertEquals(id2, active!!.id)
    }

    @Test
    fun deactivateAll_then_activate_switches_ledger() = runTest {
        val id1 = ledgerDao.insert(LedgerEntity(name = "A", icon = "a", isActive = true))
        val id2 = ledgerDao.insert(LedgerEntity(name = "B", icon = "b", isActive = false))

        // Switch from A to B
        ledgerDao.deactivateAll()
        ledgerDao.activate(id2)

        val active = ledgerDao.getActive().first()
        assertNotNull(active)
        assertEquals("B", active!!.name)

        // Verify A is no longer active
        val all = ledgerDao.getAll().first()
        val ledgerA = all.find { it.id == id1 }
        assertFalse(ledgerA!!.isActive)
    }

    @Test
    fun count_returns_correct_number() {
        assertEquals(0, ledgerDao.count())

        ledgerDao.insert(LedgerEntity(name = "A", icon = "a"))
        assertEquals(1, ledgerDao.count())

        ledgerDao.insert(LedgerEntity(name = "B", icon = "b"))
        assertEquals(2, ledgerDao.count())
    }

    @Test
    fun update_ledger() = runTest {
        val id = ledgerDao.insert(LedgerEntity(name = "旧名称", icon = "old", isActive = true))
        ledgerDao.update(LedgerEntity(id = id, name = "新名称", icon = "new", isActive = true))

        val all = ledgerDao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("新名称", all[0].name)
        assertEquals("new", all[0].icon)
    }

    @Test
    fun delete_ledger() = runTest {
        val id = ledgerDao.insert(LedgerEntity(name = "待删除", icon = "del", isActive = false))
        ledgerDao.delete(LedgerEntity(id = id, name = "待删除", icon = "del", isActive = false))

        val all = ledgerDao.getAll().first()
        assertTrue(all.isEmpty())
        assertEquals(0, ledgerDao.count())
    }
}
