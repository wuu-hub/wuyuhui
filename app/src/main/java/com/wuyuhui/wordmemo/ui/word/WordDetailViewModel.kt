package com.wuyuhui.wordmemo.ui.word

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuyuhui.wordmemo.data.local.ReviewLogEntity
import com.wuyuhui.wordmemo.data.local.WordEntity
import com.wuyuhui.wordmemo.data.repository.WordRepository
import com.wuyuhui.wordmemo.domain.scheduler.ReviewGrade
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WordDetailViewModel(
    private val repository: WordRepository,
    private val wordId: Long
) : ViewModel() {

    val word: StateFlow<WordEntity?> =
        repository.observeWord(wordId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val logs: StateFlow<List<ReviewLogEntity>> =
        repository.observeLogs(wordId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun submitReview(grade: ReviewGrade, elapsedMs: Long) {
        viewModelScope.launch {
            repository.submitReview(wordId = wordId, grade = grade, elapsedMs = elapsedMs)
        }
    }

    companion object {
        fun factory(repository: WordRepository, wordId: Long) = viewModelFactory {
            initializer { WordDetailViewModel(repository, wordId) }
        }
    }
}
