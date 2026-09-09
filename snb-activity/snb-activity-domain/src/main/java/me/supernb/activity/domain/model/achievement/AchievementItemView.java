package me.supernb.activity.domain.model.achievement;

/// 成就墙条目视图(categories[].items[] / metaAchievements[] 共用)。`hiddenReveal&&!unlocked`
/// 时 `name`/`condition`/`flavorText`/`linkUrl`/`exclusiveTag`/`revealedLabel`/`progress` 全部
/// 为 null,只有 `hint` 有值——机密档案红线,契约总览§「隐藏成就红线」。
public record AchievementItemView(
        String code,
        String name,
        String condition,
        int tier,
        int nbPoints,
        boolean unlocked,
        String status,
        boolean hiddenReveal,
        String flavorText,
        String linkUrl,
        String exclusiveTag,
        String revealedLabel,
        ProgressView progress,
        String hint) {
}
