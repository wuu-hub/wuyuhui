package com.wuyuhui.wordmemo.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuyuhui.wordmemo.data.local.WordEntity
import com.wuyuhui.wordmemo.data.repository.WordRepository
import com.wuyuhui.wordmemo.domain.scheduler.IntervalTable
import com.wuyuhui.wordmemo.ui.components.EmptyState
import com.wuyuhui.wordmemo.ui.components.formatTime

@Composable
fun LibraryDetailScreen(
    repository: WordRepository,
    libraryId: Long,
    onBack: () -> Unit,
    onOpenWord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryDetailViewModel = viewModel(
        factory = LibraryDetailViewModel.factory(repository, libraryId)
    )
    val words by viewModel.words.collectAsState()
    var spelling by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text(
                text = "${words.size} 个词条",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        Text(
            text = "词条列表",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = spelling,
                onValueChange = { spelling = it },
                label = { Text("单词") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = meaning,
                onValueChange = { meaning = it },
                label = { Text("释义") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        TextButton(
            onClick = {
                viewModel.addWord(spelling, meaning)
                spelling = ""
                meaning = ""
            }
        ) { Text("添加词条") }

        Spacer(modifier = Modifier.height(4.dp))

        if (words.isEmpty()) {
            EmptyState(
                title = "这个词库还没有词条",
                description = "在上方填写单词与释义添加，或从示例词库复制",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(words, key = { it.id }) { word ->
                    WordRow(word = word, onClick = { onOpenWord(word.id) })
                }
            }
        }
    }
}

@Composable
private fun WordRow(word: WordEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = word.spelling, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "第 ${word.intervalIndex}/${IntervalTable.maxIndex} 档",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "下次复习 ${formatTime(word.nextReviewAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
