package com.mian.accountrecord.ui.billimport

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.domain.usecase.GetLedgersUseCase
import com.mian.accountrecord.domain.usecase.ImportCsvUseCase
import com.mian.accountrecord.domain.usecase.ImportResult
import com.mian.accountrecord.util.CsvSource
import com.mian.accountrecord.util.ParsedTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importCsv: ImportCsvUseCase,
    private val getLedgers: GetLedgersUseCase
) : ViewModel() {

    data class UiState(
        val selectedFileUri: Uri? = null,
        val source: CsvSource = CsvSource.ALIPAY,
        val isLoading: Boolean = false,
        val previewList: List<ParsedTransaction> = emptyList(),
        val importResult: ImportResult? = null,
        val errorMessage: String? = null
    )

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    fun setSource(source: CsvSource) {
        _uiState.value = _uiState.value?.copy(source = source)
    }

    fun importFile(inputStream: InputStream) {
        val state = _uiState.value ?: return
        _uiState.value = state.copy(isLoading = true, errorMessage = null, importResult = null)

        viewModelScope.launch {
            try {
                val ledgers = getLedgers().first()
                val ledgerId = ledgers.find { it.isActive }?.id ?: ledgers.firstOrNull()?.id
                if (ledgerId == null) {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        errorMessage = "没有可用的账本"
                    )
                    return@launch
                }
                val result = importCsv(inputStream, state.source, ledgerId)
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    importResult = result,
                    errorMessage = if (result.errors.isNotEmpty()) result.errors.joinToString("\n") else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "导入失败"
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value?.copy(importResult = null, errorMessage = null)
    }
}
