package com.example.wordapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wordapp.data.EbbinghausScheduler
import com.example.wordapp.data.Word
import com.example.wordapp.databinding.ItemWordBinding

/** 单词本列表适配器 */
class WordAdapter(private val words: List<Word>) :
    RecyclerView.Adapter<WordAdapter.VH>() {

    class VH(val binding: ItemWordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val w = words[position]
        with(holder.binding) {
            tvWord.text = w.word
            tvMeaning.text = w.meaning
            tvStage.text = EbbinghausScheduler.stageLabel(w.stage)
            if (w.example.isNotBlank()) {
                tvExample.visibility = android.view.View.VISIBLE
                tvExample.text = w.example
            } else {
                tvExample.visibility = android.view.View.GONE
            }
        }
    }

    override fun getItemCount(): Int = words.size
}
