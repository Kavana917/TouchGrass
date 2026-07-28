package com.touchgrass.app.core.usage

import com.touchgrass.app.core.data.settings.BudgetMode

/**
 * How much of today's budget is left, and how urgently the monitor should
 * be watching.
 *
 * The [pollIntervalMillis] on each state is the adaptive-polling table from
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

/** One watched app's own allowance, in per-app mode. */
data class AppBudget(
    val packageName: String,
    val label: String = packageName,
    val budgetMinutes: Int,
    val bonusMinutes: Int,
    val usedMinutes: Int
) {
    val allowanceMinutes: Int get() = budgetMinutes + bonusMinutes
    val remainingMinutes: Int get() = (allowanceMinutes - usedMinutes).coerceAtLeast(0)
    val isSpent: Boolean get() = remainingMinutes <= 0
}

data class BudgetState(
    val dayKey: String = "",
    val mode: BudgetMode = BudgetMode.SHARED,
    /** The free daily allowance, in shared mode. */
    val budgetMinutes: Int = 0,
    /** Extra minutes bought with essays today, shared pool only. */
    val bonusMinutes: Int = 0,
    val usedMinutes: Int = 0,
    val perApp: Map<String, Int> = emptyMap(),
    /** Per-app allowances. Only meaningful in [BudgetMode.PER_APP]. */
    val appBudgets: List<AppBudget> = emptyList(),
    val foregroundPackage: String? = null,
    val watchedAppInForeground: Boolean = false,
    val screenOn: Boolean = true,
    val permissionGranted: Boolean = false,
    val monitorRunning: Boolean = false
) {
    /** Everything available today: the free budget plus anything earned. */
    val totalAllowanceMinutes: Int get() = budgetMinutes + bonusMinutes

    val remainingMinutes: Int
        get() = when (mode) {
            BudgetMode.SHARED -> (totalAllowanceMinutes - usedMinutes).coerceAtLeast(0)
            // With no single number to report, show the tightest one — that's
            // the limit the user will actually hit first.
            BudgetMode.PER_APP ->
                appBudgets.minOfOrNull { it.remainingMinutes } ?: 0
        }

    val isSpent: Boolean get() = remainingMinutes <= 0

    fun budgetFor(packageName: String?): AppBudget? =
        packageName?.let { pkg -> appBudgets.firstOrNull { it.packageName == pkg } }

    /**
     * Whether the wall should be up for the app currently in the foreground.
     *
     * In per-app mode this is the app's OWN allowance, not the total — the
     * whole point of the mode is that running out of Instagram time doesn't
     * lock you out of YouTube.
     */
    fun isSpentFor(packageName: String?): Boolean = when (mode) {
        BudgetMode.SHARED -> isSpent
        BudgetMode.PER_APP -> budgetFor(packageName)?.isSpent ?: false
    }

    val urgency: BudgetUrgency
        get() = when {
            !screenOn || !watchedAppInForeground -> BudgetUrgency.IDLE
            isSpentFor(foregroundPackage) -> BudgetUrgency.ARMED
            remainingMinutes <= 5 -> BudgetUrgency.APPROACHING
            else -> BudgetUrgency.RELAXED
        }
}
