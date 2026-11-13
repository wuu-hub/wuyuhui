package com.example.wordapp.data

/**
 * 艾宾浩斯间隔重复调度器。
 *
 * 经典遗忘曲线的复习间隔（天）：1、2、4、7、15、30。
 * 每次复习：
 *  - 记得（correct）：阶段 +1，按新阶段的间隔安排下一次复习；
 *  - 不记得（wrong）：阶段 -1（最低为 0），10 分钟后重新进入复习队列，当天再背。
 */
object EbbinghausScheduler {

    /** 各阶段对应的复习间隔（天） */
    private val INTERVALS_DAYS = intArrayOf(1, 2, 4, 7, 15, 30)

    const val LAST_STAGE = 5
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val RETAKE_DELAY_MS = 10L * 60 * 1000   // 答错后 10 分钟再来

    /** 答对后的新阶段 */
    fun stageAfterCorrect(stage: Int): Int = (stage + 1).coerceAtMost(LAST_STAGE)

    /** 答错后的新阶段 */
    fun stageAfterWrong(stage: Int): Int = (stage - 1).coerceAtLeast(0)

    /** 某阶段的间隔（毫秒） */
    fun intervalOf(stage: Int): Long =
        INTERVALS_DAYS[stage.coerceIn(0, LAST_STAGE)] * DAY_MS

    /**
     * 计算复习结果。
     * @param remember 本次是否记得
     * @param currentStage 当前阶段
     * @param now 当前时间
     * @return (新阶段, 下次复习时间)
     */
    fun schedule(remember: Boolean, currentStage: Int, now: Long): Pair<Int, Long> {
        return if (remember) {
            val newStage = stageAfterCorrect(currentStage)
            newStage to (now + intervalOf(newStage))
        } else {
            val newStage = stageAfterWrong(currentStage)
            newStage to (now + RETAKE_DELAY_MS)
        }
    }

    /** 阶段的人类可读描述，用于界面展示 */
    fun stageLabel(stage: Int): String = when {
        stage >= LAST_STAGE -> "已掌握"
        stage == 0 -> "新词"
        else -> "第${stage}轮"
    }
}
