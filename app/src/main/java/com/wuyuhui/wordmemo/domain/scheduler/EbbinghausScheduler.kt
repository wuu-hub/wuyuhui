package com.wuyuhui.wordmemo.domain.scheduler

/**
 * 复习自评档位。对应界面上的四个按钮，用户点击后决定下一次复习时间。
 */
enum class ReviewGrade(val label: String) {
    /** 完全想不起来 */
    FORGOT("忘记"),

    /** 有印象但回忆吃力 */
    VAGUE("模糊"),

    /** 顺利想起 */
    RECALLED("想起"),

    /** 脱口而出 */
    MASTERED("熟练");
}

/**
 * 基础间隔序列（艾宾浩斯曲线的工程化近似）。
 * 前 3 档为分钟级，用于当天内的短时巩固；之后进入天级间隔。
 */
object IntervalTable {

    private val MINUTE = 60_000L
    private val HOUR = 60 * MINUTE
    private val DAY = 24 * HOUR

    private val intervals: LongArray = longArrayOf(
        5 * MINUTE,
        30 * MINUTE,
        12 * HOUR,
        1 * DAY,
        2 * DAY,
        4 * DAY,
        7 * DAY,
        15 * DAY,
        30 * DAY,
        60 * DAY
    )

    val maxIndex: Int get() = intervals.lastIndex

    val size: Int get() = intervals.size

    fun intervalAt(index: Int): Long = intervals[index.coerceIn(0, maxIndex)]
}

/**
 * 一个词条当前的复习状态快照。算法只依赖它和当前时间，不依赖数据库。
 */
data class ReviewState(
    val intervalIndex: Int,
    val easeFactor: Double,
    val lapses: Int,
    val nextReviewAt: Long
)

/**
 * 复习调度算法：纯函数实现，可直接单元测试。
 *
 * 规则：
 * - 忘记：回到第 0 档，难度因子下调，忘记次数 +1；
 * - 模糊：档位不动，难度因子小幅下调（原地重复记忆）；
 * - 想起：前进一档，难度因子不变；
 * - 熟练：前进两档，难度因子小幅上调；
 * - 高档位的实际间隔 = 基础间隔 × 难度因子，低档位不放大，避免一天内间隔失真；
 * - 逾期复习时按逾期比例放大间隔，最多放大到 2 倍，防止长期中断后队列一次性堆积。
 */
object EbbinghausScheduler {

    const val DEFAULT_EASE = 2.5
    const val MIN_EASE = 1.3
    const val MAX_EASE = 3.0

    private const val EASE_STEP_DOWN_FORGOT = 0.20
    private const val EASE_STEP_DOWN_VAGUE = 0.10
    private const val EASE_STEP_UP_MASTERED = 0.15

    /** 低档位阈值：索引 <= 2 属于分钟 / 小时级，不乘难度因子 */
    private const val LOW_TIER_MAX_INDEX = 2

    private const val OVERDUE_BOOST_CAP = 2.0

    fun apply(state: ReviewState, grade: ReviewGrade, now: Long): ReviewState {
        val newIndex = nextIntervalIndex(state.intervalIndex, grade)
        val newEase = nextEaseFactor(state.easeFactor, grade)
        val newLapses = if (grade == ReviewGrade.FORGOT) state.lapses + 1 else state.lapses

        val base = IntervalTable.intervalAt(newIndex)
        val eased = if (newIndex <= LOW_TIER_MAX_INDEX) base else (base * newEase).toLong()
        val boost = overdueBoost(now, state.nextReviewAt, base)
        val delay = (eased.toDouble() * boost).toLong()

        return ReviewState(
            intervalIndex = newIndex,
            easeFactor = newEase,
            lapses = newLapses,
            nextReviewAt = now + delay
        )
    }

    fun nextIntervalIndex(current: Int, grade: ReviewGrade): Int = when (grade) {
        ReviewGrade.FORGOT -> 0
        ReviewGrade.VAGUE -> current.coerceIn(0, IntervalTable.maxIndex)
        ReviewGrade.RECALLED -> (current + 1).coerceAtMost(IntervalTable.maxIndex)
        ReviewGrade.MASTERED -> (current + 2).coerceAtMost(IntervalTable.maxIndex)
    }

    fun nextEaseFactor(current: Double, grade: ReviewGrade): Double {
        val raw = when (grade) {
            ReviewGrade.FORGOT -> current - EASE_STEP_DOWN_FORGOT
            ReviewGrade.VAGUE -> current - EASE_STEP_DOWN_VAGUE
            ReviewGrade.RECALLED -> current
            ReviewGrade.MASTERED -> current + EASE_STEP_UP_MASTERED
        }
        return raw.coerceIn(MIN_EASE, MAX_EASE)
    }

    /**
     * 逾期放大系数。未逾期返回 1.0；逾期越久系数越大，上限 2.0。
     */
    fun overdueBoost(now: Long, dueAt: Long, baseInterval: Long): Double {
        if (baseInterval <= 0L) return 1.0
        val overdue = now - dueAt
        if (overdue <= 0L) return 1.0
        val ratio = overdue.toDouble() / baseInterval.toDouble()
        return (1.0 + ratio).coerceAtMost(OVERDUE_BOOST_CAP)
    }

    /**
     * 新词首次学习后的状态：学完立刻进入第 0 档。
     */
    fun initialState(now: Long, intervalIndex: Int = 0): ReviewState = ReviewState(
        intervalIndex = intervalIndex,
        easeFactor = DEFAULT_EASE,
        lapses = 0,
        nextReviewAt = now + IntervalTable.intervalAt(intervalIndex)
    )
}
