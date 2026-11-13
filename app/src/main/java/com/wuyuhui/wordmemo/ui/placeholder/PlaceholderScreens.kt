package com.wuyuhui.wordmemo.ui.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 骨架阶段的占位页面：明确写出后续里程碑要接入的能力，
 * 既满足三态中的空态展示，也方便现场检查时说明进度。
 */
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    upcoming: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "后续接入", style = MaterialTheme.typography.titleSmall)
                upcoming.forEach { item ->
                    Text(
                        text = "· $item",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun TodayScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "今日",
        subtitle = "当前版本已打通数据层与复习算法，今日队列在里程碑 6 接入",
        upcoming = listOf(
            "按 nextReviewAt 生成今日到期队列",
            "每日新词上限与到期复习的配比",
            "连续学习天数统计"
        ),
        modifier = modifier
    )
}

@Composable
fun QuizScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "测试",
        subtitle = "拼写题与选择题在里程碑 6 接入，算法侧已具备错题回退能力",
        upcoming = listOf(
            "拼写题：给出释义输入拼写",
            "选择题：干扰项同词库抽取",
            "错词自动回退到低档间隔"
        ),
        modifier = modifier
    )
}

@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "统计",
        subtitle = "统计与分享入口在里程碑 8 接入，届时联动伴生模块 P4",
        upcoming = listOf(
            "复习量、正确率、连续天数",
            "未来 7 天到期预测",
            "跳转伴生模块生成分享卡片"
        ),
        modifier = modifier
    )
}
