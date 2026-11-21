package com.example.wordapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wordapp.data.WordRepository
import com.example.wordapp.databinding.ActivityWordListBinding

/** 单词本：展示全部单词及当前复习阶段，点条目进入词条详情 */
class WordListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val words = WordRepository.get(this).getAllWords()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = WordAdapter(words) { w ->
            startActivity(
                Intent(this, WordDetailActivity::class.java)
                    .putExtra(WordDetailActivity.EXTRA_WORD_ID, w.id)
            )
        }
        binding.tvCount.text = "共 ${words.size} 个"

        binding.btnBack.setOnClickListener { finish() }
    }
}
