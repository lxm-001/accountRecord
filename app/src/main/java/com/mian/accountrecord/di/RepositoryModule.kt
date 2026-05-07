package com.mian.accountrecord.di

import com.mian.accountrecord.data.repository.AuthRepositoryImpl
import com.mian.accountrecord.data.repository.BudgetRepositoryImpl
import com.mian.accountrecord.data.repository.CategoryRepositoryImpl
import com.mian.accountrecord.data.repository.LedgerRepositoryImpl
import com.mian.accountrecord.data.repository.TransactionRepositoryImpl
import com.mian.accountrecord.domain.repository.AuthRepository
import com.mian.accountrecord.domain.repository.BudgetRepository
import com.mian.accountrecord.domain.repository.CategoryRepository
import com.mian.accountrecord.domain.repository.LedgerRepository
import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.util.CsvParser
import com.mian.accountrecord.util.CsvParserImpl
import com.mian.accountrecord.util.CsvPrinter
import com.mian.accountrecord.util.CsvPrinterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    abstract fun bindLedgerRepository(
        impl: LedgerRepositoryImpl
    ): LedgerRepository

    @Binds
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    abstract fun bindCsvParser(
        impl: CsvParserImpl
    ): CsvParser

    @Binds
    abstract fun bindCsvPrinter(
        impl: CsvPrinterImpl
    ): CsvPrinter
}
