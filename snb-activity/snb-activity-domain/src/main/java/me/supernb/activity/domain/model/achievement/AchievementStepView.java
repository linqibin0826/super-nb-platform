package me.supernb.activity.domain.model.achievement;

/// 系列内单层视图(categories[].series[].steps[]),字段集比 AchievementItemView 窄——
/// MVP 42 条里没有"隐藏系列",故不带 hint/linkUrl/exclusiveTag/revealedLabel/progress。
public record AchievementStepView(
        String code,
        String name,
        String condition,
        int tier,
        int nbPoints,
        boolean unlocked,
        String status,
        boolean hiddenReveal,
        String flavorText) {
}
