package com.mian.accountrecord.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mian.accountrecord.data.local.entity.BudgetEntity
import com.mian.accountrecord.data.local.entity.CategoryEntity
import com.mian.accountrecord.data.local.entity.Converters
import com.mian.accountrecord.data.local.entity.LedgerEntity
import com.mian.accountrecord.data.local.entity.TransactionEntity
import com.mian.accountrecord.data.local.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        LedgerEntity::class,
        BudgetEntity::class,
        UserEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AccountRecordDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun budgetDao(): BudgetDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "account_record.db"

        fun createCallback(database: () -> AccountRecordDatabase): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        val instance = database()
                        insertPresetCategories(instance.categoryDao())
                        insertDefaultLedger(instance.ledgerDao())
                    }
                }
            }
        }

        private suspend fun insertPresetCategories(categoryDao: CategoryDao) {
            val expenseCategories = listOf(
                CategoryEntity(name = "餐饮", icon = "restaurant", color = "#FF5722", type = 0, isPreset = true, sortOrder = 0),
                CategoryEntity(name = "交通", icon = "directions_bus", color = "#2196F3", type = 0, isPreset = true, sortOrder = 1),
                CategoryEntity(name = "购物", icon = "shopping_bag", color = "#E91E63", type = 0, isPreset = true, sortOrder = 2),
                CategoryEntity(name = "住房", icon = "home", color = "#795548", type = 0, isPreset = true, sortOrder = 3),
                CategoryEntity(name = "娱乐", icon = "sports_esports", color = "#9C27B0", type = 0, isPreset = true, sortOrder = 4),
                CategoryEntity(name = "医疗", icon = "local_hospital", color = "#F44336", type = 0, isPreset = true, sortOrder = 5),
                CategoryEntity(name = "教育", icon = "school", color = "#3F51B5", type = 0, isPreset = true, sortOrder = 6),
                CategoryEntity(name = "通讯", icon = "phone", color = "#00BCD4", type = 0, isPreset = true, sortOrder = 7),
                CategoryEntity(name = "服饰", icon = "checkroom", color = "#FF9800", type = 0, isPreset = true, sortOrder = 8),
                CategoryEntity(name = "日用", icon = "local_grocery_store", color = "#4CAF50", type = 0, isPreset = true, sortOrder = 9),
                CategoryEntity(name = "社交", icon = "people", color = "#673AB7", type = 0, isPreset = true, sortOrder = 10),
                CategoryEntity(name = "宠物", icon = "pets", color = "#8BC34A", type = 0, isPreset = true, sortOrder = 11),
                CategoryEntity(name = "其他", icon = "more_horiz", color = "#607D8B", type = 0, isPreset = true, sortOrder = 12)
            )

            val incomeCategories = listOf(
                CategoryEntity(name = "工资", icon = "account_balance_wallet", color = "#4CAF50", type = 1, isPreset = true, sortOrder = 0),
                CategoryEntity(name = "奖金", icon = "emoji_events", color = "#FFC107", type = 1, isPreset = true, sortOrder = 1),
                CategoryEntity(name = "兼职", icon = "work", color = "#FF9800", type = 1, isPreset = true, sortOrder = 2),
                CategoryEntity(name = "投资收益", icon = "trending_up", color = "#2196F3", type = 1, isPreset = true, sortOrder = 3),
                CategoryEntity(name = "红包", icon = "redeem", color = "#F44336", type = 1, isPreset = true, sortOrder = 4),
                CategoryEntity(name = "其他", icon = "more_horiz", color = "#607D8B", type = 1, isPreset = true, sortOrder = 5)
            )

            expenseCategories.forEach { categoryDao.insert(it) }
            incomeCategories.forEach { categoryDao.insert(it) }
        }

        private suspend fun insertDefaultLedger(ledgerDao: LedgerDao) {
            ledgerDao.insert(
                LedgerEntity(
                    name = "日常账本",
                    icon = "home",
                    template = "daily",
                    isActive = true
                )
            )
        }
    }
}
