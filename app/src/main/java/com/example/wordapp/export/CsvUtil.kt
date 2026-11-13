package com.example.wordapp.export

import com.example.wordapp.data.Word
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * CSV 工具：词库导入解析 + 单词数据导出。
 * 支持双引号包裹、字段内逗号、引号转义（""），UTF-8 编码。
 * 导入格式：word,meaning,example（第三列可省略，表头自动识别跳过）。
 */
object CsvUtil {

    /** 解析输入流中的 CSV 为单词列表 */
    fun parse(input: InputStream): List<Word> {
        val text = input.bufferedReader(StandardCharsets.UTF_8).readText()
        val rows = parseRows(text)
        if (rows.isEmpty()) return emptyList()

        val dataRows = if (looksLikeHeader(rows.first())) rows.drop(1) else rows
        val now = System.currentTimeMillis()

        return dataRows.mapNotNull { cols ->
            val word = cols.getOrNull(0)?.trim().orEmpty()
            val meaning = cols.getOrNull(1)?.trim().orEmpty()
            if (word.isEmpty() || meaning.isEmpty()) return@mapNotNull null
            Word(
                word = word,
                meaning = meaning,
                example = cols.getOrNull(2)?.trim().orEmpty(),
                createdAt = now
            )
        }.distinctBy { it.word.lowercase() }
    }

    private fun looksLikeHeader(cols: List<String>): Boolean {
        val joined = cols.joinToString(",").lowercase()
        return ("word" in joined || "单词" in joined) &&
            ("meaning" in joined || "释义" in joined || "中文" in joined)
    }

    /** 把单词列表序列化为 CSV 文本（含表头） */
    fun toCsv(words: List<Word>): String {
        val sb = StringBuilder()
        sb.appendLine("word,meaning,example,stage,learned,review_count,correct_count,next_review_at")
        for (w in words) {
            sb.append(escape(w.word)).append(',')
            sb.append(escape(w.meaning)).append(',')
            sb.append(escape(w.example)).append(',')
            sb.append(w.stage).append(',')
            sb.append(if (w.learned) 1 else 0).append(',')
            sb.append(w.reviewCount).append(',')
            sb.append(w.correctCount).append(',')
            sb.append(w.nextReviewAt)
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun escape(field: String): String {
        return if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else field
    }

    /**
     * 通用 CSV 行解析：按字符扫描，正确处理引号与转义。
     */
    private fun parseRows(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        val row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var fieldStarted = false

        fun pushField() {
            row.add(field.toString())
            field.setLength(0)
            fieldStarted = false
        }
        fun pushRow() {
            pushField()
            rows.add(ArrayList(row))
            row.clear()
        }

        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes && ch == '"' -> {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"'); i++
                    } else {
                        inQuotes = false
                    }
                }
                !inQuotes && ch == '"' -> {
                    inQuotes = true; fieldStarted = true
                }
                !inQuotes && ch == ',' -> {
                    pushField()
                }
                !inQuotes && (ch == '\n' || ch == '\r') -> {
                    // 处理 CRLF
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    if (fieldStarted || row.isNotEmpty()) pushRow()
                }
                else -> {
                    field.append(ch); fieldStarted = true
                }
            }
            i++
        }
        // 最后一行（无换行结尾）
        if (fieldStarted || row.isNotEmpty()) {
            pushRow()
        }
        return rows
    }
}
