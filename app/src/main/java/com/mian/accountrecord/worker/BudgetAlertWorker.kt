package com.mian.accountrecord.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mian.accountrecord.domain.repository.LedgerRepository
import com.mian.accountrecord.domain.usecase.CheckBudgetAlertUseCase
import com.mian.accountrecord.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.YearMonth

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkBudgetAlert: CheckBudgetAlertUseCase,
    private val ledgerRepository: LedgerRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val ledgers = ledgerRepository.getAll().first()
            val activeLedger = ledgers.find { it.isActive } ?: ledgers.firstOrNull()
                ?: return Result.success()

            val alerts = checkBudgetAlert(activeLedger.id, YearMonth.now())
            alerts.forEach { alert ->
                NotificationHelper.showBudgetAlert(applicationContext, alert)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
