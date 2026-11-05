package com.example.wordapp.data

/**
 * 单词实体，对应 SQLite 表 words。
 * stage 为艾宾浩斯复习阶段，决定下一次复习的间隔天数。
 */
data class Word(
    val id: Long = 0,
    val word: String,
    val meaning: String,
    val example: String = "",
    val stage: Int = 0,
    val learned: Boolean = false,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewAt: Long = 0,
    val nextReviewAt: Long = 0
) {
    /** 达到最后一个阶段视为已掌握 */
    val mastered: Boolean get() = stage >= EbbinghausScheduler.LAST_STAGE
}
