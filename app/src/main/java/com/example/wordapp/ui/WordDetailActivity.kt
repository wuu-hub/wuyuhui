package com.example.wordapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.wordapp.data.EbbinghausScheduler
import com.example.wordapp.data.WordRepository
import com.example.wordapp.databinding.ActivityWordDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 词条详情：从单词本列表点进来，展示释义、例句、复习记录与下次复习时间。
 * 数据只读，不提供编辑，避免与复习页产生双写。
 */
class WordDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_WORD_ID = "extra_word_id"

        private val TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    }

    private lateinit var binding: ActivityWordDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val wordId = intent.getLongExtra(EXTRA_WORD_ID, 0L)
        val word = WordRepository.get(this).wordById(wordId)
        if (word == null) {
            toast("这个单词已经不在词库里了")
            finish()
            return
        }

        with(binding) {
            tvWord.text = word.word
            tvStage.text = if (word.learned)
                EbbinghausScheduler.stageLabel(word.stage) else "新词"
            tvMeaning.text = word.meaning

            if (word.example.isNotBlank()) {
                tvExample.visibility = View.VISIBLE
                tvExample.text = word.example
            } else {
                tvExample.visibility = View.GONE
            }

            tvReviewCount.text = word.reviewCount.toString()
            tvCorrectCount.text = word.correctCount.toString()
            tvRate.text = if (word.reviewCount > 0) {
                "${Math.round(word.correctCount * 100f / word.reviewCount)}%"
            } else {
                "—"
            }

            tvLastReview.text = if (word.lastReviewAt > 0) {
                "上次复习：${TIME_FORMAT.format(Date(word.lastReviewAt))}"
            } else {
                "上次复习：还没复习过"
            }

            tvNextReview.text = when {
                !word.learned -> "下次复习：学完第一个，就会自动安排"
                word.nextReviewAt <= 0 -> "下次复习：未安排"
                word.nextReviewAt <= System.currentTimeMillis() -> "下次复习：已到期，可以开始复习"
                else -> "下次复习：${TIME_FORMAT.format(Date(word.nextReviewAt))}"
            }

            btnBack.setOnClickListener { finish() }
            btnDetailBack.setOnClickListener { finish() }
            btnGoReview.setOnClickListener {
                // 带上当前词 id，复习页就只跑这一个词，不复用整轮队列
                val intent = Intent(this@WordDetailActivity, ReviewActivity::class.java)
                    .putExtra(ReviewActivity.EXTRA_WORD_ID, word.id)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}
