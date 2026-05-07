package com.mian.accountrecord.di

import android.content.Context
import androidx.room.Room
import com.mian.accountrecord.data.local.db.AccountRecordDatabase
import com.mian.accountrecord.data.local.db.BudgetDao
import com.mian.accountrecord.data.local.db.CategoryDao
import com.mian.accountrecord.data.local.db.LedgerDao
import com.mian.accountrecord.data.local.db.TransactionDao
import com.mian.accountrecord.data.local.db.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AccountRecordDatabase {
        lateinit var database: AccountRecordDatabase
        database = Room.databaseBuilder(
            context,
            AccountRecordDatabase::class.java,
            AccountRecordDatabase.DATABASE_NAME
        )
            .addCallback(AccountRecordDatabase.createCallback { database })
            .fallbackToDestructiveMigration()
            .build()
        return database
    }

    @Provides
    fun provideTransactionDao(db: AccountRecordDatabase): TransactionDao {
        return db.transactionDao()
    }

    @Provides
    fun provideCategoryDao(db: AccountRecordDatabase): CategoryDao {
        return db.categoryDao()
    }

    @Provides
    fun provideLedgerDao(db: AccountRecordDatabase): LedgerDao {
        return db.ledgerDao()
    }

    @Provides
    fun provideBudgetDao(db: AccountRecordDatabase): BudgetDao {
        return db.budgetDao()
    }

    @Provides
    fun provideUserDao(db: AccountRecordDatabase): UserDao {
        return db.userDao()
    }
}
