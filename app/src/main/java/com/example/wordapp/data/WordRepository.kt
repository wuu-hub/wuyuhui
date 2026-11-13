package com.example.wordapp.data

import android.content.Context

/** 首页统计数据 */
data class Stats(
    val total: Int,
    val learned: Int,
    val mastered: Int,
    val due: Int,
    val correctRate: Int
)

/**
 * 单词仓库：统一的数据访问入口，封装艾宾浩斯调度业务。
 */
class WordRepository private constructor(private val context: Context) {

    private val db = DbHelper.get(context)

    companion object {
        @Volatile
        private var instance: WordRepository? = null

        fun get(context: Context): WordRepository =
            instance ?: synchronized(this) {
                instance ?: WordRepository(context.applicationContext).also {
                    it.seedIfNeeded()
                }
            }
    }

    /** 首次启动预置示例词库，保证打开即可体验 */
    private fun seedIfNeeded() {
        val prefs = context.getSharedPreferences("wordapp", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return
        val now = System.currentTimeMillis()
        val seeds = listOf(
            Word(word = "abandon", meaning = "v. 放弃；抛弃", example = "Never abandon your dream.", createdAt = now),
            Word(word = "benefit", meaning = "n. 益处 v. 受益", example = "Reading benefits us a lot.", createdAt = now),
            Word(word = "capacity", meaning = "n. 容量；能力", example = "The hall has a capacity of 500.", createdAt = now),
            Word(word = "decline", meaning = "v./n. 下降；婉拒", example = "Sales declined last month.", createdAt = now),
            Word(word = "efficient", meaning = "adj. 高效的", example = "She is an efficient worker.", createdAt = now),
            Word(word = "familiar", meaning = "adj. 熟悉的", example = "Your face looks familiar.", createdAt = now),
            Word(word = "generate", meaning = "v. 产生；发电", example = "The plant generates electricity.", createdAt = now),
            Word(word = "hesitate", meaning = "v. 犹豫", example = "Don't hesitate to ask me.", createdAt = now),
            Word(word = "improve", meaning = "v. 改善；提高", example = "I want to improve my English.", createdAt = now),
            Word(word = "justify", meaning = "v. 证明……正当", example = "Nothing can justify the lie.", createdAt = now),
            Word(word = "knowledge", meaning = "n. 知识；学问", example = "Knowledge is power.", createdAt = now),
            Word(word = "launch", meaning = "v. 发射；发起", example = "They will launch a new app.", createdAt = now),
            Word(word = "maintain", meaning = "v. 维持；保养", example = "Maintain a healthy diet.", createdAt = now),
            Word(word = "negotiate", meaning = "v. 谈判；协商", example = "We negotiate the price.", createdAt = now),
            Word(word = "obvious", meaning = "adj. 明显的", example = "The answer is obvious.", createdAt = now),
            Word(word = "persuade", meaning = "v. 说服", example = "He persuaded me to join.", createdAt = now),
            Word(word = "quality", meaning = "n. 质量；品质", example = "Quality matters more than quantity.", createdAt = now),
            Word(word = "recommend", meaning = "v. 推荐", example = "I recommend this movie.", createdAt = now),
            Word(word = "schedule", meaning = "n. 日程 v. 安排", example = "The schedule is tight.", createdAt = now),
            Word(word = "tremendous", meaning = "adj. 巨大的；极好的", example = "A tremendous effort was made.", createdAt = now)
        )
        db.bulkInsert(seeds)
        prefs.edit().putBoolean("seeded", true).apply()
    }

    /** 导入词库，返回新增条数（重复单词自动跳过） */
    fun importWords(words: List<Word>): Int = db.bulkInsert(words)

    fun getAllWords(): List<Word> = db.getAllWords()

    /**
     * 取本次要学习/复习的队列：到期复习词 + 未学新词（新词最多补 10 个）。
     */
    fun getStudyQueue(newWordLimit: Int = 10): List<Word> {
        val now = System.currentTimeMillis()
        val due = db.getDueWords(now)
        val fresh = db.getNewWords(newWordLimit)
        return due + fresh
    }

    fun dueCount(): Int = db.countDue(System.currentTimeMillis())

    /**
     * 提交一次复习结果，内部走艾宾浩斯调度。
     * @return 更新后的单词
     */
    fun review(word: Word, remember: Boolean): Word {
        val now = System.currentTimeMillis()
        val (newStage, nextAt) = EbbinghausScheduler.schedule(remember, word.stage, now)
        db.applyReview(word.id, newStage, remember, nextAt, now)
        return db.getById(word.id) ?: word
    }

    /** 测试出题：至少需要 4 个单词才能凑齐选项 */
    fun quizWords(questionCount: Int): List<Word> {
        val all = db.getRandomWords(questionCount.coerceAtLeast(4))
        return all
    }

    fun distractors(question: Word, count: Int = 3): List<String> {
        val picked = db.getDistractorMeanings(question.id, count).toMutableList()
        // 词库太小时用占位项补齐，保证始终有 4 个选项
        var k = 1
        while (picked.size < count) {
            val fake = "（干扰项 $k）"
            if (fake !in picked) picked.add(fake)
            k++
        }
        return picked
    }

    fun stats(): Stats {
        val now = System.currentTimeMillis()
        return Stats(
            total = db.count(),
            learned = db.countLearned(),
            mastered = db.countMastered(),
            due = db.countDue(now),
            correctRate = db.overallCorrectRate()
        )
    }
}
