package com.mian.accountrecord.ui.ledger

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.domain.model.Ledger
import com.mian.accountrecord.domain.model.LedgerTemplate
import com.mian.accountrecord.domain.usecase.CreateLedgerUseCase
import com.mian.accountrecord.domain.usecase.DeleteLedgerUseCase
import com.mian.accountrecord.domain.usecase.GetLedgersUseCase
import com.mian.accountrecord.domain.usecase.SwitchLedgerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val getLedgers: GetLedgersUseCase,
    private val createLedger: CreateLedgerUseCase,
    private val switchLedger: SwitchLedgerUseCase,
    private val deleteLedger: DeleteLedgerUseCase
) : ViewModel() {

    data class UiState(
        val ledgers: List<Ledger> = emptyList(),
        val errorMessage: String? = null
    )

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    init {
        viewModelScope.launch {
            getLedgers().collectLatest { ledgers ->
                _uiState.value = _uiState.value?.copy(ledgers = ledgers)
            }
        }
    }

    fun create(name: String, icon: String, template: LedgerTemplate) {
        viewModelScope.launch {
            val ledger = Ledger(name = name, icon = icon, template = template)
            val result = createLedger(ledger)
            if (result.isFailure) {
                _uiState.value = _uiState.value?.copy(
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun switchTo(ledgerId: Long) {
        viewModelScope.launch { switchLedger(ledgerId) }
    }

    fun delete(ledger: Ledger) {
        viewModelScope.launch {
            val result = deleteLedger(ledger)
            if (result.isFailure) {
                _uiState.value = _uiState.value?.copy(
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }
}
