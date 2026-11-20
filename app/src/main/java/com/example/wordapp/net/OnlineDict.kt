package com.example.wordapp.net

import com.example.wordapp.data.Word
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 在线查词：调用公开的 Free Dictionary API 拉取单词释义，
 * 查到后写进本地词库，之后照常参与艾宾浩斯复习。
 *
 * 接口无需申请 key，返回 JSON 数组，取第一条词条的释义与例句。
 */
object OnlineDict {

    private const val BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en/"
    private const val TIMEOUT_MS = 15000

    /** 查询结果：成功带回一个可直接入库的 Word，失败带回给用户看的原因 */
    sealed class Result {
        data class Success(val word: Word) : Result()
        data class Failure(val reason: String) : Result()
    }

    /**
     * 查询一个英文单词，阻塞式，必须放到子线程调用。
     * 网络异常、查无此词、服务器报错都转成 Failure，不往外抛。
     */
    fun lookup(raw: String): Result {
        val term = raw.trim().lowercase()
        if (term.isEmpty()) return Result.Failure("请先输入要查询的单词")
        if (!term.matches(Regex("[a-z][a-z'\\- ]{0,30}"))) {
            return Result.Failure("只能查询英文单词")
        }

        var conn: HttpURLConnection? = null
        return try {
            val url = URL(BASE_URL + URLEncoder.encode(term, "UTF-8"))
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            when (val code = conn.responseCode) {
                200 -> parse(conn.inputStream.bufferedReader().use { it.readText() }, term)
                404 -> Result.Failure("词典里查不到“$term”，检查拼写或换个单词")
                in 500..599 -> Result.Failure("词典服务器暂时不可用（$code），稍后再试")
                else -> Result.Failure("请求失败（HTTP $code）")
            }
        } catch (e: java.net.UnknownHostException) {
            Result.Failure("连不上词典服务器，请检查网络或 DNS")
        } catch (e: java.net.SocketTimeoutException) {
            Result.Failure("网络超时，请稍后重试")
        } catch (e: Exception) {
            Result.Failure("网络请求出错：${e.message ?: e.javaClass.simpleName}")
        } finally {
            conn?.disconnect()
        }
    }

    /** 解析词典返回，拼成“词性 释义；”的形式 */
    private fun parse(body: String, term: String): Result {
        val arr = JSONArray(body)
        if (arr.length() == 0) return Result.Failure("词典没有返回内容")

        val entry = arr.getJSONObject(0)
        val word = entry.optString("word", term)
        val phonetic = entry.optString("phonetic", "")

        val meanings = entry.optJSONArray("meanings") ?: JSONArray()
        val meaning = StringBuilder()
        var example = ""

        for (i in 0 until minOf(meanings.length(), 3)) {
            val m = meanings.getJSONObject(i)
            val pos = abbreviation(m.optString("partOfSpeech", ""))
            val defs = m.optJSONArray("definitions") ?: continue
            for (j in 0 until minOf(defs.length(), 2)) {
                val d = defs.getJSONObject(j)
                val text = d.optString("definition", "").trim()
                if (text.isEmpty()) continue
                if (meaning.isNotEmpty()) meaning.append("；")
                meaning.append(pos).append(" ").append(text)
                if (example.isEmpty()) {
                    example = d.optString("example", "").trim()
                }
            }
        }

        if (meaning.isEmpty()) return Result.Failure("词典返回的释义为空，换个单词试试")

        val fullMeaning = if (phonetic.isNotEmpty()) "$phonetic  $meaning" else meaning.toString()
        return Result.Success(
            Word(
                word = word,
                meaning = fullMeaning,
                example = example,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /** 词性缩写，让释义更紧凑 */
    private fun abbreviation(pos: String): String = when (pos) {
        "noun" -> "n."
        "verb" -> "v."
        "adjective" -> "adj."
        "adverb" -> "adv."
        "pronoun" -> "pron."
        "preposition" -> "prep."
        "conjunction" -> "conj."
        else -> if (pos.isEmpty()) "释义" else "$pos"
    }
}
