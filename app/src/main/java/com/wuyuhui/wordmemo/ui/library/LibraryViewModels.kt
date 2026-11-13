package com.wuyuhui.wordmemo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuyuhui.wordmemo.data.local.WordEntity
import com.wuyuhui.wordmemo.data.local.WordLibraryEntity
import com.wuyuhui.wordmemo.data.repository.WordRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryListViewModel(private val repository: WordRepository) : ViewModel() {

    val libraries: StateFlow<List<WordLibraryEntity>> =
        repository.observeLibraries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.ensureSeedData() }
    }

    fun createLibrary(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.createLibrary(name = trimmed, description = "手动新建的词库")
        }
    }

    companion object {
        fun factory(repository: WordRepository) = viewModelFactory {
            initializer { LibraryListViewModel(repository) }
        }
    }
}

class LibraryDetailViewModel(
    private val repository: WordRepository,
    private val libraryId: Long
) : ViewModel() {

    val words: StateFlow<List<WordEntity>> =
        repository.observeWords(libraryId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addWord(spelling: String, meaning: String) {
        val s = spelling.trim()
        val m = meaning.trim()
        if (s.isEmpty() || m.isEmpty()) return
        viewModelScope.launch {
            repository.addWord(libraryId = libraryId, spelling = s, meaning = m)
        }
    }

    companion object {
        fun factory(repository: WordRepository, libraryId: Long) = viewModelFactory {
            initializer { LibraryDetailViewModel(repository, libraryId) }
        }
    }
}
