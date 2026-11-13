package com.wuyuhui.wordmemo.data.repository

import com.wuyuhui.wordmemo.data.local.ReviewLogDao
import com.wuyuhui.wordmemo.data.local.ReviewLogEntity
import com.wuyuhui.wordmemo.data.local.WordDao
import com.wuyuhui.wordmemo.data.local.WordEntity
import com.wuyuhui.wordmemo.data.local.WordLibraryDao
import com.wuyuhui.wordmemo.data.local.WordLibraryEntity
import com.wuyuhui.wordmemo.domain.scheduler.EbbinghausScheduler
import com.wuyuhui.wordmemo.domain.scheduler.ReviewGrade
import com.wuyuhui.wordmemo.domain.scheduler.ReviewState
import kotlinx.coroutines.flow.Flow

/**
 * 仓库层：UI 只与它交互，不直接接触 DAO。
 * 复习调度的算法调用集中在这里，保证「写入状态」与「记录日志」是一次操作。
 */
class WordRepository(
    private val libraryDao: WordLibraryDao,
    private val wordDao: WordDao,
    private val reviewLogDao: ReviewLogDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    fun observeLibraries(): Flow<List<WordLibraryEntity>> = libraryDao.observeAll()

    fun observeWords(libraryId: Long): Flow<List<WordEntity>> = wordDao.observeByLibrary(libraryId)

    fun observeWord(wordId: Long): Flow<WordEntity?> = wordDao.observeById(wordId)

    fun observeWordCount(libraryId: Long): Flow<Int> = wordDao.observeCount(libraryId)

    fun observeDueCount(libraryId: Long): Flow<Int> = wordDao.observeDueCount(libraryId, clock())

    fun observeLogs(wordId: Long): Flow<List<ReviewLogEntity>> = reviewLogDao.observeForWord(wordId)

    suspend fun createLibrary(
        name: String,
        description: String = "",
        source: String = WordLibraryEntity.SOURCE_MANUAL
    ): Long = libraryDao.insert(
        WordLibraryEntity(
            name = name,
            description = description,
            source = source,
            createdAt = clock()
        )
    )

    suspend fun addWord(libraryId: Long, spelling: String, meaning: String, example: String? = null): Long {
        val now = clock()
        return wordDao.insert(
            WordEntity(
                libraryId = libraryId,
                spelling = spelling,
                meaning = meaning,
                example = example,
                intervalIndex = 0,
                nextReviewAt = now + com.wuyuhui.wordmemo.domain.scheduler.IntervalTable.intervalAt(0),
                easeFactor = EbbinghausScheduler.DEFAULT_EASE,
                createdAt = now
            )
        )
    }

    /**
     * 提交一次复习：按自评档位重算状态，并追加一条复习记录。
     * 返回新的到期时间，null 表示词条不存在。
     */
    suspend fun submitReview(wordId: Long, grade: ReviewGrade, elapsedMs: Long): Long? {
        val word = wordDao.findById(wordId) ?: return null
        val now = clock()
        val before = ReviewState(
            intervalIndex = word.intervalIndex,
            easeFactor = word.easeFactor,
            lapses = word.lapses,
            nextReviewAt = word.nextReviewAt
        )
        val after = EbbinghausScheduler.apply(before, grade, now)

        wordDao.update(
            word.copy(
                intervalIndex = after.intervalIndex,
                easeFactor = after.easeFactor,
                lapses = after.lapses,
                nextReviewAt = after.nextReviewAt
            )
        )

        reviewLogDao.insert(
            ReviewLogEntity(
                wordId = wordId,
                reviewedAt = now,
                grade = grade.name,
                previousIntervalIndex = before.intervalIndex,
                newIntervalIndex = after.intervalIndex,
                previousEaseFactor = before.easeFactor,
                newEaseFactor = after.easeFactor,
                elapsedMs = elapsedMs
            )
        )
        return after.nextReviewAt
    }

    /**
     * 首次启动写入示例词库，保证空态下有可操作的入口。
     */
    suspend fun ensureSeedData() {
        if (libraryDao.count() > 0) return
        val libraryId = createLibrary(
            name = "四级核心词（示例）",
            description = "首次启动内置的示例词库，可直接开始复习",
            source = WordLibraryEntity.SOURCE_BUILT_IN
        )
        seedWords.forEach { (spelling, meaning, example) ->
            addWord(libraryId, spelling, meaning, example)
        }
    }

    private val seedWords: List<Triple<String, String, String>> = listOf(
        Triple("abandon", "v. 放弃；抛弃", "They had to abandon the plan."),
        Triple("benefit", "n./v. 利益；有益的", "Regular exercise benefits health."),
        Triple("capable", "adj. 有能力的", "She is capable of solving it."),
        Triple("decline", "v./n. 下降；婉拒", "Sales declined last quarter."),
        Triple("efficient", "adj. 高效的", "An efficient way to learn words."),
        Triple("fragile", "adj. 易碎的；脆弱的", "Handle the glass — it is fragile.")
    )
}
