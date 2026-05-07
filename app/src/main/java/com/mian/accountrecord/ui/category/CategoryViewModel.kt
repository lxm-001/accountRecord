package com.mian.accountrecord.ui.category

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mian.accountrecord.domain.model.Category
import com.mian.accountrecord.domain.model.TransactionType
import com.mian.accountrecord.domain.usecase.AddCategoryUseCase
import com.mian.accountrecord.domain.usecase.DeleteCategoryUseCase
import com.mian.accountrecord.domain.usecase.GetCategoriesUseCase
import com.mian.accountrecord.domain.usecase.ReorderCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val addCategory: AddCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val reorderCategories: ReorderCategoriesUseCase
) : ViewModel() {

    data class UiState(
        val currentType: TransactionType = TransactionType.EXPENSE,
        val categories: List<Category> = emptyList(),
        val errorMessage: String? = null
    )

    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> = _uiState

    private var collectJob: Job? = null

    init {
        loadCategories(TransactionType.EXPENSE)
    }

    fun switchType(type: TransactionType) {
        _uiState.value = _uiState.value?.copy(currentType = type)
        loadCategories(type)
    }

    private fun loadCategories(type: TransactionType) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            getCategories(type).collectLatest { cats ->
                _uiState.value = _uiState.value?.copy(categories = cats)
            }
        }
    }

    fun addCategory(name: String, icon: String, color: String) {
        val state = _uiState.value ?: return
        viewModelScope.launch {
            val category = Category(
                name = name,
                icon = icon,
                color = color,
                type = state.currentType,
                sortOrder = state.categories.size
            )
            val result = addCategory.invoke(category)
            if (result.isFailure) {
                _uiState.value = state.copy(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val result = deleteCategory.invoke(category)
            if (result.isFailure) {
                _uiState.value = _uiState.value?.copy(
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun reorder(reorderedList: List<Category>) {
        _uiState.value = _uiState.value?.copy(categories = reorderedList)
        viewModelScope.launch {
            reorderCategories(reorderedList)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }
}
