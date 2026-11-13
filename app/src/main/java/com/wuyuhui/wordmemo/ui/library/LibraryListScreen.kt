package com.wuyuhui.wordmemo.ui.library

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuyuhui.wordmemo.data.local.WordLibraryEntity
import com.wuyuhui.wordmemo.data.repository.WordRepository
import com.wuyuhui.wordmemo.ui.components.EmptyState
import com.wuyuhui.wordmemo.ui.components.formatTime

@Composable
fun LibraryListScreen(
    repository: WordRepository,
    onOpenLibrary: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryListViewModel = viewModel(
        factory = LibraryListViewModel.factory(repository)
    )
    val libraries by viewModel.libraries.collectAsState()
    var newName by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = "词库",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
        )
        Text(
            text = "共 ${libraries.size} 个词库",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("新建词库名称") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    viewModel.createLibrary(newName)
                    newName = ""
                }
            ) { Text("新建") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (libraries.isEmpty()) {
            EmptyState(
                title = "还没有词库",
                description = "在上方输入名称新建一个，或等待内置示例词库加载完成",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(libraries, key = { it.id }) { library ->
                    LibraryCard(library = library, onClick = { onOpenLibrary(library.id) })
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(library: WordLibraryEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = library.name, style = MaterialTheme.typography.titleMedium)
            if (library.description.isNotBlank()) {
                Text(
                    text = library.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = sourceLabel(library.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "创建于 ${formatTime(library.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun sourceLabel(source: String): String = when (source) {
    WordLibraryEntity.SOURCE_BUILT_IN -> "内置"
    WordLibraryEntity.SOURCE_IMPORTED -> "导入"
    else -> "手动"
}
