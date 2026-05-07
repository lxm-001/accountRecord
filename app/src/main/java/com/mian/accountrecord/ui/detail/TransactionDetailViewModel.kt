package com.mian.accountrecord.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.domain.model.Transaction
import com.mian.accountrecord.domain.repository.TransactionRepository
import com.mian.accountrecord.domain.usecase.DeleteTransactionUseCase
import com.mian.accountrecord.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    data class UiState(
        val transaction: Transaction? = null,
        val isDeleted: Boolean = false
    )

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    private val transactionId: Long = savedStateHandle.get<Long>("id") ?: 0L

    init {
        viewModelScope.launch {
            transactionRepository.getById(transactionId).collectLatest { tx ->
                _uiState.value = _uiState.value?.copy(transaction = tx)
            }
        }
    }

    fun update(transaction: Transaction) {
        viewModelScope.launch {
            updateTransaction(transaction)
        }
    }

    fun delete() {
        val tx = _uiState.value?.transaction ?: return
        viewModelScope.launch {
            deleteTransaction(tx)
            _uiState.value = _uiState.value?.copy(isDeleted = true)
        }
    }
}
