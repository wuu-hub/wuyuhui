package com.wuyuhui.wordmemo.domain.scheduler

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 调度算法的行为锁定测试。
 * 算法是纯函数，不依赖 Android 框架，所以直接用 JVM 单元测试验证。
 */
class EbbinghausSchedulerTest {

    private val now = 1_700_000_000_000L
    private val day = 24 * 60 * 60 * 1000L

    private fun state(
        intervalIndex: Int = 0,
        ease: Double = EbbinghausScheduler.DEFAULT_EASE,
        lapses: Int = 0,
        dueAt: Long = now
    ) = ReviewState(intervalIndex = intervalIndex, easeFactor = ease, lapses = lapses, nextReviewAt = dueAt)

    @Test
    fun `忘记时回到第 0 档并累计失误次数`() {
        val before = state(intervalIndex = 5, ease = 2.5, lapses = 1)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.FORGOT, now)

        assertEquals(0, after.intervalIndex)
        assertEquals(2, after.lapses)
        assertEquals(2.3, after.easeFactor, 1e-9)
    }

    @Test
    fun `模糊时档位不动，难度因子小幅下降`() {
        val before = state(intervalIndex = 4, ease = 2.5)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.VAGUE, now)

        assertEquals(4, after.intervalIndex)
        assertEquals(2.4, after.easeFactor, 1e-9)
    }

    @Test
    fun `想起时前进一档，难度因子不变`() {
        val before = state(intervalIndex = 3, ease = 2.5)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.RECALLED, now)

        assertEquals(4, after.intervalIndex)
        assertEquals(2.5, after.easeFactor, 1e-9)
    }

    @Test
    fun `熟练时前进两档，难度因子小幅上升`() {
        val before = state(intervalIndex = 3, ease = 2.5)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.MASTERED, now)

        assertEquals(5, after.intervalIndex)
        assertEquals(2.65, after.easeFactor, 1e-9)
    }

    @Test
    fun `档位封顶于最大间隔`() {
        val before = state(intervalIndex = IntervalTable.maxIndex, ease = 2.9)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.MASTERED, now)

        assertEquals(IntervalTable.maxIndex, after.intervalIndex)
        assertEquals(EbbinghausScheduler.MAX_EASE, after.easeFactor, 1e-9)
    }

    @Test
    fun `难度因子有下限，连续忘记不会低于 1_3`() {
        var s = state(intervalIndex = 2, ease = 2.5)
        repeat(10) {
            s = EbbinghausScheduler.apply(s, ReviewGrade.FORGOT, now)
        }
        assertEquals(EbbinghausScheduler.MIN_EASE, s.easeFactor, 1e-9)
    }

    @Test
    fun `未逾期时按基础间隔安排下次复习`() {
        val before = state(intervalIndex = 0, dueAt = now)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.RECALLED, now)

        val base = IntervalTable.intervalAt(1)
        assertEquals(now + base, after.nextReviewAt)
    }

    @Test
    fun `高档位间隔按难度因子放大`() {
        val before = state(intervalIndex = 4, ease = 2.5, dueAt = now)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.RECALLED, now)

        val base = IntervalTable.intervalAt(5)
        val expected = now + (base * 2.5).toLong()
        assertEquals(expected, after.nextReviewAt)
    }

    @Test
    fun `逾期放大系数上限为 2 倍`() {
        val base = IntervalTable.intervalAt(3)
        val boost = EbbinghausScheduler.overdueBoost(now + base * 10, now, base)
        assertEquals(2.0, boost, 1e-9)
    }

    @Test
    fun `逾期越久间隔越长但不超过两倍`() {
        val before = state(intervalIndex = 4, ease = 2.5, dueAt = now - 20 * day)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.RECALLED, now)

        val base = IntervalTable.intervalAt(5)
        val eased = (base * 2.5).toLong()
        val maxDelay = (eased * 2.0).toLong()
        assertEquals(now + maxDelay, after.nextReviewAt)
    }

    @Test
    fun `逾期补偿不会缩短正常间隔`() {
        val before = state(intervalIndex = 3, ease = 2.5, dueAt = now + day)
        val after = EbbinghausScheduler.apply(before, ReviewGrade.RECALLED, now)

        val base = IntervalTable.intervalAt(4)
        assertEquals(now + (base * 2.5).toLong(), after.nextReviewAt)
    }

    @Test
    fun `新词初始状态落在第 0 档`() {
        val init = EbbinghausScheduler.initialState(now)
        assertEquals(0, init.intervalIndex)
        assertEquals(EbbinghausScheduler.DEFAULT_EASE, init.easeFactor, 1e-9)
        assertEquals(0, init.lapses)
        assertEquals(now + IntervalTable.intervalAt(0), init.nextReviewAt)
    }
}
