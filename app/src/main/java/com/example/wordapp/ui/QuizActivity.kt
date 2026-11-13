package com.example.wordapp.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.wordapp.data.Word
import com.example.wordapp.data.WordRepository
import com.example.wordapp.databinding.ActivityQuizBinding
import com.google.android.material.button.MaterialButton

/**
 * 单词测试：看英文单词，从 4 个释义中选出正确答案。
 * 答完给出得分与正确率，测试结果不影响艾宾浩斯复习调度。
 */
class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding
    private lateinit var questions: List<Word>

    private var index = 0
    private var score = 0
    private var locked = false

    private val optionButtons: List<MaterialButton> by lazy {
        listOf(binding.btnOpt1, binding.btnOpt2, binding.btnOpt3, binding.btnOpt4)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        questions = WordRepository.get(this).quizWords(10)

        if (questions.size < 4) {
            binding.layoutQuiz.visibility = View.GONE
            binding.layoutResult.visibility = View.VISIBLE
            binding.tvScore.text = "词库不足"
            binding.tvQuizDetail.text = "至少需要 4 个单词才能开始测试，请先导入词库。"
            binding.btnQuizBack.setOnClickListener { finish() }
            return
        }

        optionButtons.forEach { btn ->
            btn.setOnClickListener { onOptionClick(btn) }
        }
        binding.btnQuizBack.setOnClickListener { finish() }

        renderQuestion()
    }

    private fun renderQuestion() {
        locked = false
        val q = questions[index]
        binding.tvQuestion.text = q.word
        binding.tvQuizProgress.text = "第 ${index + 1} / ${questions.size} 题"

        val options = (repository().distractors(q, 3) + q.meaning).shuffled()
        optionButtons.forEachIndexed { i, btn ->
            btn.text = options[i]
            btn.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#3F6BE0"))
            btn.isEnabled = true
        }
    }

    private fun repository() = WordRepository.get(this)

    private fun onOptionClick(btn: MaterialButton) {
        if (locked) return
        locked = true

        val q = questions[index]
        val isCorrect = btn.text == q.meaning
        if (isCorrect) score++

        // 高亮正确答案，错误选择标红
        optionButtons.forEach { b ->
            b.isEnabled = false
            if (b.text == q.meaning) {
                b.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#2EAD6B"))
            }
        }
        if (!isCorrect) {
            btn.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#E5574E"))
        }

        binding.root.postDelayed({
            index++
            if (index >= questions.size) showResult() else renderQuestion()
        }, 900)
    }

    private fun showResult() {
        binding.layoutQuiz.visibility = View.GONE
        binding.layoutResult.visibility = View.VISIBLE
        val rate = Math.round(score * 100f / questions.size)
        binding.tvScore.text = "$score / ${questions.size} 分"
        binding.tvQuizDetail.text = "正确率 $rate%，继续加油！"
    }
}
