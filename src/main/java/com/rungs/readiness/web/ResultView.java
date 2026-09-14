package com.rungs.readiness.web;

import java.util.List;

/**
 * The result page's view model (design.md 038 section 4): the verdict, the three stats, and the seven-stage ladder.
 *
 * @param eyebrow "You're on stage N of 7", or "All seven stages are done or skipped" when stage 7 is satisfied
 * @param verdictLine the current stage's headline
 * @param verdictSub the current stage's supporting line
 * @param monthlySurplus the monthly surplus, formatted
 * @param surplusPositive whether the surplus is above $0, so the stat renders in green
 * @param nonMortgageDebt the total consumer debt balance, formatted
 * @param stagesComplete "K of 7", where K counts only the stages that are satisfied
 * @param stages all seven stage rows, in order
 */
record ResultView(
        String eyebrow,
        String verdictLine,
        String verdictSub,
        String monthlySurplus,
        boolean surplusPositive,
        String nonMortgageDebt,
        String stagesComplete,
        List<StageView> stages) {
}
