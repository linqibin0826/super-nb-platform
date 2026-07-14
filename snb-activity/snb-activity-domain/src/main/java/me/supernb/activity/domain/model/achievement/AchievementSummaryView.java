package me.supernb.activity.domain.model.achievement;

/// 墙面总览计数。nbTotal 是用户已解锁全部成就的 pointsAtUnlock 之和(冻结值,不随后续
/// 该成就本身改档而变——深化稿"points_at_unlock 定格"不可逆决策)。
public record AchievementSummaryView(int unlockedCount, int totalCount, int nbTotal) {
}
