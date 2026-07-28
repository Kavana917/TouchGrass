package com.touchgrass.app.core.usage

/**
 * How much of today's budget is left, and how urgently the monitor should
 * be watching.
 *
 * The [pollInterval] on each state is the adaptive-polling table from
 * app_plan.md §2.7, expressed as code.
 *
 * WHY ADAPTIVE: the usual complaint about UsageStatsManager is "it's too
 * slow to block with". That's a polling problem, not an API problem. You
 * only need fast detection when the wall is armed — when the user has 25
 * minutes left, a few seconds of imprecision in a 30-minute budget is
 * irrelevant. So we poll lazily most of the time and tighten up only when
 * it matters, which gets a wall that lands in about a second without paying
 * for it in battery all day.
 */
enum class BudgetUrgency(val pollIntervalMillis: Long) {
    /** Plenty left. Cheap polling — we're only accumulating. */
    RELAXED(8_000L),

    /** Under 5 minutes. Getting ready to fire. */
    APPROACHING(2_000L),

    /** Zero. The wall is armed and needs to land before they're absorbed. */
    ARMED(1_000L),

    /** Screen off, or no watched app in the foreground. Costs nothing. */
    IDLE(30_000L)
}

data class BudgetState(
    val dayKey: String = "",
    /** The free daily allowance. */
    val budgetMinutes: Int = 0,
    /** Extra minutes bought with essays today (Phase 3). */
    val bonusMinutes: Int = 0,
    val usedMinutes: Int = 0,
    val perApp: Map<String, Int> = emptyMap(),
    val foregroundPackage: String? = null,
    val watchedAppInForeground: Boolean = false,
    val screenOn: Boolean = true,
    val permissionGranted: Boolean = false,
    val monitorRunning: Boolean = false
) {
    /** Everything available today: the free budget plus anything earned. */
    val totalAllowanceMinutes: Int get() = budgetMinutes + bonusMinutes

    val remainingMinutes: Int
        get() = (totalAllowanceMinutes - usedMinutes).coerceAtLeast(0)

    val isSpent: Boolean get() = remainingMinutes <= 0

    val urgency: BudgetUrgency
        get() = when {
            !screenOn || !watchedAppInForeground -> BudgetUrgency.IDLE
            isSpent -> BudgetUrgency.ARMED
            remainingMinutes <= 5 -> BudgetUrgency.APPROACHING
            else -> BudgetUrgency.RELAXED
        }
}
