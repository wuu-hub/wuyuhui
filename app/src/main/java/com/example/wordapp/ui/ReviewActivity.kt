package com.example.wordapp.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.wordapp.data.EbbinghausScheduler
import com.example.wordapp.data.Word
import com.example.wordapp.data.WordRepository
import com.example.wordapp.databinding.ActivityReviewBinding

/**
 * 复习页：按复习队列逐词卡片学习，
 * “认识/不认识”的结果通过艾宾浩斯调度器更新下一次复习时间。
 */
class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReviewBinding
    private lateinit var repository: WordRepository
    private lateinit var queue: MutableList<Word>

    /** 本轮已追加过的错词，避免反复错导致无限循环 */
    private val retried = mutableSetOf<Long>()

    private var index = 0
    private var remembered = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WordRepository.get(this)
        queue = repository.getStudyQueue().toMutableList()

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

    private fun answer(remember: Boolean) {
        val current = queue[index]
        val updated = repository.review(current, remember)
        if (remember) {
            remembered++
        } else if (retried.add(current.id)) {
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
        binding.layoutStudy.visibility = View.GONE
        binding.layoutDone.visibility = View.VISIBLE
        binding.tvDoneSummary.text = if (empty) {
            "词库还是空的，先导入词库或添加单词吧。"
        } else {
            "本次共复习 ${queue.size} 次，\n其中记得 $remembered 次，继续保持！"
        }
    }
}
