package me.supernb.activity.domain.model.achievement;

import java.util.List;

/// 一个类目分组(如"入职档案"/"机密档案")。`hidden` 派生自"本类目全部成就是否都
/// hidden_reveal=true"(机密档案由内容设计天然满足,非 DB 显式标注的独立位)。
public record AchievementCategoryView(
        String name, boolean hidden, List<AchievementItemView> items, List<AchievementSeriesView> series) {
}
