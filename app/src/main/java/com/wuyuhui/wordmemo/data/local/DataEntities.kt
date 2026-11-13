package com.wuyuhui.wordmemo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 词库。一个词库对应一个学习单元，词条通过外键挂在词库下。
 */
@Entity(tableName = "word_libraries")
data class WordLibraryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    /** 来源：BUILT_IN 内置示例 / IMPORTED 文件导入 / MANUAL 手动新建 */
    val source: String = SOURCE_MANUAL,
    val createdAt: Long
) {
    companion object {
        const val SOURCE_BUILT_IN = "BUILT_IN"
        const val SOURCE_IMPORTED = "IMPORTED"
        const val SOURCE_MANUAL = "MANUAL"
    }
}

/**
 * 词条。复习调度的状态全部保存在这里，避免每次复习都回溯历史记录重算。
 */
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = WordLibraryEntity::class,
            parentColumns = ["id"],
            childColumns = ["libraryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("libraryId"),
        Index("nextReviewAt"),
        Index(value = ["libraryId", "spelling"], unique = true)
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val libraryId: Long,
    val spelling: String,
    val meaning: String,
    val example: String? = null,
    val phonetic: String? = null,
    /** 当前所处间隔档位，0 为最短间隔 */
    val intervalIndex: Int = 0,
    /** 下一次应当复习的时刻（epoch millis） */
    val nextReviewAt: Long = 0L,
    /** 难度因子，决定高档位间隔的放大倍数 */
    val easeFactor: Double = 2.5,
    /** 忘记次数，用于统计与长期评估 */
    val lapses: Int = 0,
    val isStarred: Boolean = false,
    val createdAt: Long = 0L
)

/**
 * 复习记录。只追加不修改，作为算法行为的可追溯证据。
 */
@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("wordId"), Index("reviewedAt")]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val wordId: Long,
    val reviewedAt: Long,
    /** 自评档位，存枚举名称 */
    val grade: String,
    val previousIntervalIndex: Int,
    val newIntervalIndex: Int,
    val previousEaseFactor: Double,
    val newEaseFactor: Double,
    val elapsedMs: Long
)
