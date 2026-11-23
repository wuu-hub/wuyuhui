package com.example.wordapp.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.wordapp.data.EbbinghausScheduler
import com.example.wordapp.data.Word
import com.example.wordapp.data.WordRepository
import com.example.wordapp.databinding.ActivityReviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 复习页：按复习队列逐词卡片学习，
 * “认识/不认识”的结果通过艾宾浩斯调度器更新下一次复习时间。
 */
class ReviewActivity : AppCompatActivity() {

    companion object {
        /** 从词条详情页“只复习这个词”带过来的单词 id */
        const val EXTRA_WORD_ID = "extra_word_id"

        private const val PREFS = "wordapp_review"
        private const val KEY_IDS = "ids"
        private const val KEY_INDEX = "index"
        private const val KEY_REMEMBERED = "remembered"
        private const val KEY_RETRIED = "retried"
        private const val KEY_TIME = "saved_at"

        /** 未完成的一轮复习最多保留一天，过期重新开始 */
        private const val SESSION_TTL_MS = 24L * 60 * 60 * 1000

        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    }

    private lateinit var binding: ActivityReviewBinding
    private lateinit var repository: WordRepository
    private lateinit var queue: MutableList<Word>

    /** 本轮已追加过的错词，避免反复错导致无限循环 */
    private val retried = mutableSetOf<Long>()

    /** 只复习详情页指定的那一个词（单词语习模式），不参与整轮进度落盘 */
    private var singleMode = false

    private var index = 0
    private var remembered = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WordRepository.get(this)

        val singleId = intent.getLongExtra(EXTRA_WORD_ID, 0L)
        val singleWord = if (singleId > 0) repository.wordById(singleId) else null

        queue = if (singleWord != null) {
            singleMode = true
            mutableListOf(singleWord)
        } else {
            // 上次没答完的队列优先续上，避免切后台回来从头开始
            restoreSession() ?: repository.getStudyQueue().toMutableList()
        }

        if (queue.isEmpty()) {
            showDone(empty = true)
            return
        }

        binding.btnShow.setOnClickListener {
            binding.tvMeaning.visibility = View.VISIBLE
            binding.tvExample.visibility = View.VISIBLE
            it.visibility = View.GONE
        }
        binding.btnCorrect.setOnClickListener { answer(true) }
        binding.btnWrong.setOnClickListener { answer(false) }
        binding.btnDoneBack.setOnClickListener { finish() }

        render()
    }

    override fun onPause() {
        super.onPause()
        // 单词语习只走一个词，不该覆盖掉整轮复习的续答现场
        if (singleMode) return
        // 只在“答到一半”时保存，答完或没开始都不需要留现场
        if (index in 1 until queue.size) saveSession() else clearSession()
    }

    /** 有可续答的会话就返回重建好的队列，否则返回 null 走新队列 */
    private fun restoreSession(): MutableList<Word>? {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val idsText = prefs.getString(KEY_IDS, null) ?: return null
        val savedIndex = prefs.getInt(KEY_INDEX, 0)
        if (idsText.isEmpty() || savedIndex <= 0) return null

        val savedAt = prefs.getLong(KEY_TIME, 0)
        if (System.currentTimeMillis() - savedAt > SESSION_TTL_MS) {
            clearSession()
            return null
        }

        val ids = idsText.split(",").mapNotNull { it.toLongOrNull() }
        if (savedIndex >= ids.size) {
            clearSession()
            return null
        }

        val words = ids.mapNotNull { repository.wordById(it) }
        if (words.size != ids.size) {
            clearSession()
            return null
        }

        index = savedIndex
        remembered = prefs.getInt(KEY_REMEMBERED, 0)
        prefs.getString(KEY_RETRIED, "")?.split(",")?.forEach {
            it.toLongOrNull()?.let(retried::add)
        }
        return words.toMutableList()
    }

    private fun saveSession() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_IDS, queue.joinToString(",") { it.id.toString() })
            .putInt(KEY_INDEX, index)
            .putInt(KEY_REMEMBERED, remembered)
            .putString(KEY_RETRIED, retried.joinToString(","))
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    private fun clearSession() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun answer(remember: Boolean) {
        val current = queue[index]
        val updated = repository.review(current, remember)
        if (remember) {
            remembered++
        } else if (!singleMode && retried.add(current.id)) {
            // 答错的词排到本轮末尾再出现一次，当天加固记忆
            queue.add(updated)
        }

        index++
        if (index >= queue.size) {
            showDone(empty = false)
        } else {
            render()
        }
    }

    private fun render() {
        val w = queue[index]
        binding.tvWord.text = w.word
        binding.tvStage.text = if (w.learned)
            EbbinghausScheduler.stageLabel(w.stage) else "新词"
        binding.tvMeaning.text = w.meaning
        binding.tvExample.text = w.example

        // 每个新词默认隐藏释义
        binding.tvMeaning.visibility = View.INVISIBLE
        binding.tvExample.visibility = View.INVISIBLE
        binding.btnShow.visibility = View.VISIBLE

        binding.tvProgress.text = "${index + 1} / ${queue.size}"
        binding.progressBar.progress =
            ((index + 1).toFloat() / queue.size * 100).toInt()
    }

    private fun showDone(empty: Boolean) {
        if (!singleMode) clearSession()
        binding.layoutStudy.visibility = View.GONE
        binding.layoutDone.visibility = View.VISIBLE
        binding.tvDoneSummary.text = when {
            // 单词语习：直接把调度算出来的下次复习时间摆给用户看
            singleMode -> {
                val done = repository.wordById(queue.last().id) ?: queue.last()
                val stage = if (done.learned) EbbinghausScheduler.stageLabel(done.stage) else "新词"
                if (done.nextReviewAt > 0) {
                    "「${done.word}」已复习完，当前$stage，\n下次复习：${TIME_FORMAT.format(Date(done.nextReviewAt))}"
                } else {
                    "「${done.word}」已复习完。"
                }
            }
            !empty -> "本次共复习 ${queue.size} 次，\n其中记得 $remembered 次，继续保持！"
            // 词库本身没词，和“今天都复习完了”是两回事，提示要分开
            repository.stats().total == 0 -> "词库还是空的，先导入词库或添加单词吧。"
            else -> "今天的复习都完成了，\n明天再来巩固吧。"
        }
    }
}
