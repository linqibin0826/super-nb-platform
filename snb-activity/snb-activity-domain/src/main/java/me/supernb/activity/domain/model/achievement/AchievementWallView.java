package me.supernb.activity.domain.model.achievement;

import java.util.List;

/// 成就墙整页视图。`metaAchievements` 是"元编年史"类目单独抽出(不进 categories[]);
/// `pendingUnseal` 是"已解锁但 seen=false"的 code 简单数组,具体名称在 categories/
/// metaAchievements 里已给出,不重复带一份。
public record AchievementWallView(
        AchievementSummaryView summary,
        List<AchievementCategoryView> categories,
        List<AchievementItemView> metaAchievements,
        List<String> pendingUnseal) {
}
