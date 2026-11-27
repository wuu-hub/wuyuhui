package com.example.wordapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wordapp.data.Word
import com.example.wordapp.data.WordRepository
import com.example.wordapp.databinding.ActivityWordListBinding

/** 单词本：展示全部单词及当前复习阶段，点条目进入详情，长按可删除 */
class WordListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWordListBinding
    private val repository by lazy { WordRepository.get(this) }
    private lateinit var words: MutableList<Word>
    private lateinit var adapter: WordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        words = repository.getAllWords().toMutableList()
        adapter = WordAdapter(words, onItemClick = { w -> openDetail(w) }, onItemLongClick = { w -> confirmDelete(w) })
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        updateCount()

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun openDetail(word: Word) {
        startActivity(
            Intent(this, WordDetailActivity::class.java)
                .putExtra(WordDetailActivity.EXTRA_WORD_ID, word.id)
        )
    }

    /** 长按词条：二次确认后删除，避免误触把背过的记录清掉 */
    private fun confirmDelete(word: Word) {
        AlertDialog.Builder(this)
            .setTitle("删除单词")
            .setMessage("确定把「${word.word}」从词库中删除吗？\n它的复习记录会一起清除。")
            .setPositiveButton("删除") { _, _ ->
                if (repository.deleteWord(word.id)) {
                    words.removeAll { it.id == word.id }
                    adapter.notifyDataSetChanged()
                    updateCount()
                    Toast.makeText(this, "已删除「${word.word}」", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "删除失败，可能已经不存在了", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateCount() {
        binding.tvCount.text = "共 ${words.size} 个"
    }
}
