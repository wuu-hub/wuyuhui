package com.example.wordapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 单词数据库，原生 SQLite 实现，无需 Room 依赖。
 */
class DbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "wordapp.db"
        private const val DB_VERSION = 2
        const val TABLE = "words"

        @Volatile
        private var instance: DbHelper? = null

        fun get(context: Context): DbHelper =
            instance ?: synchronized(this) {
                instance ?: DbHelper(context.applicationContext).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                word TEXT NOT NULL,
                meaning TEXT NOT NULL,
                example TEXT NOT NULL DEFAULT '',
                stage INTEGER NOT NULL DEFAULT 0,
                learned INTEGER NOT NULL DEFAULT 0,
                review_count INTEGER NOT NULL DEFAULT 0,
                correct_count INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                last_review_at INTEGER NOT NULL DEFAULT 0,
                next_review_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        // 单词唯一，导入时去重
        db.execSQL("CREATE UNIQUE INDEX idx_word ON $TABLE(word)")
        createQueryIndexes(db)
    }

    /** 查询相关的索引：到期队列按 (learned, next_review_at) 过滤排序，测试按 stage 抽题 */
    private fun createQueryIndexes(db: SQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_due ON $TABLE(learned, next_review_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stage ON $TABLE(stage)")
    }

    /**
     * 升级策略：只做增量迁移，不再 DROP 表。
     * 早期版本用 DROP + CREATE 升级，升一次版本就把用户已背的词全清空，
     * 这里改成按版本号补索引，数据一律保留。
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 1 -> 2：补查询索引
        if (oldVersion < 2) createQueryIndexes(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 降级同样保留数据，索引留在库里也无害
    }

    /** 插入一个单词，重复单词返回 -1 */
    fun insert(word: Word): Long {
        val cv = baseValues(word)
        return writableDatabase.insertWithOnConflict(TABLE, null, cv,
            SQLiteDatabase.CONFLICT_IGNORE)
    }

    /** 批量插入，返回实际新增条数 */
    fun bulkInsert(words: List<Word>): Int {
        val db = writableDatabase
        var added = 0
        db.beginTransaction()
        try {
            for (w in words) {
                val id = db.insertWithOnConflict(TABLE, null, baseValues(w),
                    SQLiteDatabase.CONFLICT_IGNORE)
                if (id != -1L) added++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return added
    }

    private fun baseValues(w: Word) = ContentValues().apply {
        put("word", w.word.trim())
        put("meaning", w.meaning.trim())
        put("example", w.example.trim())
        put("stage", w.stage)
        put("learned", if (w.learned) 1 else 0)
        put("review_count", w.reviewCount)
        put("correct_count", w.correctCount)
        put("created_at", w.createdAt)
        put("last_review_at", w.lastReviewAt)
        put("next_review_at", w.nextReviewAt)
    }

    /** 提交一次复习结果 */
    fun applyReview(id: Long, newStage: Int, remember: Boolean, nextAt: Long, now: Long) {
        writableDatabase.execSQL(
            """
            UPDATE $TABLE SET
                stage = ?, learned = 1,
                review_count = review_count + 1,
                correct_count = correct_count + ?,
                last_review_at = ?, next_review_at = ?
            WHERE _id = ?
            """.trimIndent(),
            arrayOf<Any>(newStage, if (remember) 1 else 0, now, nextAt, id)
        )
    }

    /** 到期复习队列：next_review_at <= now，越早到期越靠前；limit 限制单次带入条数 */
    fun getDueWords(now: Long, limit: Int = Int.MAX_VALUE): List<Word> {
        val result = ArrayList<Word>()
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE WHERE learned = 1 AND next_review_at <= ? " +
                "ORDER BY next_review_at ASC LIMIT ?",
            arrayOf(now.toString(), limit.toString())
        )
        c.use { while (it.moveToNext()) result.add(it.toWord()) }
        return result
    }

    /** 还没学过的新词（首次学习队列） */
    fun getNewWords(limit: Int): List<Word> {
        val result = ArrayList<Word>()
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE WHERE learned = 0 ORDER BY created_at ASC LIMIT ?",
            arrayOf(limit.toString())
        )
        c.use { while (it.moveToNext()) result.add(it.toWord()) }
        return result
    }

    fun getAllWords(): List<Word> {
        val result = ArrayList<Word>()
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE ORDER BY next_review_at ASC", null)
        c.use { while (it.moveToNext()) result.add(it.toWord()) }
        return result
    }

    fun getById(id: Long): Word? {
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE WHERE _id = ?", arrayOf(id.toString()))
        return c.use { if (it.moveToFirst()) it.toWord() else null }
    }

    /** 删除单个词条，返回是否真的删掉了一条 */
    fun deleteById(id: Long): Boolean =
        writableDatabase.delete(TABLE, "_id = ?", arrayOf(id.toString())) > 0

    /** 随机取若干单词，用于测试出题 */
    fun getRandomWords(limit: Int): List<Word> {
        val result = ArrayList<Word>()
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE ORDER BY RANDOM() LIMIT ?", arrayOf(limit.toString()))
        c.use { while (it.moveToNext()) result.add(it.toWord()) }
        return result
    }

    /** 随机取尚未掌握的单词（stage < 已掌握），用于测试优先出未掌握的词 */
    fun getRandomWordsUnmastered(limit: Int): List<Word> {
        val result = ArrayList<Word>()
        val c = readableDatabase.rawQuery(
            "SELECT * FROM $TABLE WHERE stage < ? ORDER BY RANDOM() LIMIT ?",
            arrayOf(EbbinghausScheduler.LAST_STAGE.toString(), limit.toString())
        )
        c.use { while (it.moveToNext()) result.add(it.toWord()) }
        return result
    }

    /** 取干扰项释义（排除正确答案本身） */
    fun getDistractorMeanings(excludeId: Long, limit: Int): List<String> {
        val result = ArrayList<String>()
        val c = readableDatabase.rawQuery(
            "SELECT meaning FROM $TABLE WHERE _id != ? ORDER BY RANDOM() LIMIT ?",
            arrayOf(excludeId.toString(), limit.toString())
        )
        c.use { while (it.moveToNext()) result.add(it.getString(0)) }
        return result
    }

    fun count(): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE", null)
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun countLearned(): Int = countWhere("learned = 1")
    fun countMastered(): Int = countWhere("stage >= ${EbbinghausScheduler.LAST_STAGE}")
    fun countDue(now: Long): Int {
        val c = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE WHERE learned = 1 AND next_review_at <= ?",
            arrayOf(now.toString()))
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    private fun countWhere(where: String): Int {
        val c = readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE WHERE $where", null)
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /** 整体答题正确率（0~100），没有复习记录时返回 0 */
    fun overallCorrectRate(): Int {
        val c = readableDatabase.rawQuery(
            "SELECT SUM(correct_count), SUM(review_count) FROM $TABLE", null)
        return c.use {
            if (it.moveToFirst() && !it.isNull(1) && it.getInt(1) > 0)
                Math.round(it.getInt(0) * 100f / it.getInt(1))
            else 0
        }
    }

    private fun android.database.Cursor.toWord(): Word = Word(
        id = getLong(getColumnIndexOrThrow("_id")),
        word = getString(getColumnIndexOrThrow("word")),
        meaning = getString(getColumnIndexOrThrow("meaning")),
        example = getString(getColumnIndexOrThrow("example")),
        stage = getInt(getColumnIndexOrThrow("stage")),
        learned = getInt(getColumnIndexOrThrow("learned")) == 1,
        reviewCount = getInt(getColumnIndexOrThrow("review_count")),
        correctCount = getInt(getColumnIndexOrThrow("correct_count")),
        createdAt = getLong(getColumnIndexOrThrow("created_at")),
        lastReviewAt = getLong(getColumnIndexOrThrow("last_review_at")),
        nextReviewAt = getLong(getColumnIndexOrThrow("next_review_at"))
    )
}
