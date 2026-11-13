package com.wuyuhui.wordmemo.ui.word

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuyuhui.wordmemo.data.repository.WordRepository
import com.wuyuhui.wordmemo.domain.scheduler.IntervalTable
import com.wuyuhui.wordmemo.domain.scheduler.ReviewGrade
import com.wuyuhui.wordmemo.ui.components.EmptyState
import com.wuyuhui.wordmemo.ui.components.formatTime

@Composable
fun WordDetailScreen(
    repository: WordRepository,
    wordId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: WordDetailViewModel = viewModel(
        factory = WordDetailViewModel.factory(repository, wordId)
    )
    val word by viewModel.word.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val startedAt = remember { System.currentTimeMillis() }

    val current = word
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(onClick = onBack) { Text("← 返回") }
        }

        if (current == null) {
            EmptyState(
                title = "词条不存在",
                description = "可能已被删除，返回词库列表查看",
                modifier = Modifier.height(400.dp)
            )
            return@Column
        }

        Text(text = current.spelling, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = current.meaning,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (!current.example.isNullOrBlank()) {
            Text(
                text = current.example,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "复习状态", style = MaterialTheme.typography.titleSmall)
                InfoLine("当前档位", "${current.intervalIndex} / ${IntervalTable.maxIndex}（基础间隔 ${describeInterval(current.intervalIndex)}）")
                InfoLine("下次复习", formatTime(current.nextReviewAt))
                InfoLine("难度因子", "%.2f".format(current.easeFactor))
                InfoLine("忘记次数", "${current.lapses} 次")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "本次回忆表现", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "选择后立即按算法重算下次复习时间，并写入复习记录",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReviewGrade.entries.forEach { grade ->
                OutlinedButton(
                    onClick = {
                        viewModel.submitReview(
                            grade = grade,
                            elapsedMs = System.currentTimeMillis() - startedAt
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(grade.label) }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "复习记录（${logs.size}）", style = MaterialTheme.typography.titleSmall)
        if (logs.isEmpty()) {
            Text(
                text = "还没有复习记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            logs.take(10).forEach { log ->
                Text(
                    text = "${formatTime(log.reviewedAt)} · ${log.grade} · 档位 ${log.previousIntervalIndex}→${log.newIntervalIndex}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun describeInterval(index: Int): String {
    val minutes = IntervalTable.intervalAt(index) / 60_000L
    return when {
        minutes < 60 -> "$minutes 分钟"
        minutes < 60 * 24 -> "${minutes / 60} 小时"
        else -> "${minutes / (60 * 24)} 天"
    }
}
