package com.jaeyun.tally.ui.screens.appclass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jaeyun.tally.appContainer
import com.jaeyun.tally.data.repository.AppClassificationRepository
import com.jaeyun.tally.data.room.AppClassification
import com.jaeyun.tally.domain.model.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

/** 앱 분류 화면의 한 줄 */
data class AppRow(val packageName: String, val appLabel: String, val category: Category)

/**
 * 감지된 앱을 이름 순서로. 한국어 사전 순서(가나다)를 따르고 영문 이름도 대소문자와 관계없이 섞는다.
 * 토글해도 줄이 움직이지 않도록 분류로 나누지 않는다.
 */
internal fun appRows(apps: List<AppClassification>, locale: Locale = Locale.KOREAN): List<AppRow> {
    val collator = Collator.getInstance(locale)
    return apps
        .sortedWith(compareBy(collator) { it: AppClassification -> it.appLabel }.thenBy { it.packageName })
        .map { AppRow(it.packageName, it.appLabel, it.category) }
}

sealed interface AppClassificationUiState {
    data object Loading : AppClassificationUiState

    /** 아직 끝낸 세션이 없거나, 세션 중에 다른 앱을 연 적이 없다 */
    data object Empty : AppClassificationUiState

    data class Content(val rows: List<AppRow>) : AppClassificationUiState
}

/**
 * 앱 분류 (§5.1, 와이어프레임 §7). 세션에서 감지된 앱을 딴짓·허용으로 바꾼다. 바꾸면 사용자 분류가 되어 자동 분류가 덮어쓰지
 * 않고, 다음 세션부터 반영된다(지난 세션은 다시 계산하지 않는다 — PLAN.md §4).
 */
class AppClassificationViewModel(private val repository: AppClassificationRepository) : ViewModel() {

    val uiState: StateFlow<AppClassificationUiState> = repository.detectedApps
        .map { apps -> if (apps.isEmpty()) AppClassificationUiState.Empty else AppClassificationUiState.Content(appRows(apps)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppClassificationUiState.Loading)

    fun setCategory(packageName: String, category: Category) {
        viewModelScope.launch { repository.setCategory(packageName, category) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { AppClassificationViewModel(appContainer.appClassificationRepository) }
        }
    }
}
