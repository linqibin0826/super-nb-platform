package me.supernb.activity.domain.model.achievement;

import java.math.BigDecimal;
import java.time.LocalDate;

/// 成就目录条目(领域读视图,来自 achievement_definition,字段与 DB 列一一对应)。
/// `conditionText` 追加在末尾(而非按 DB 列序插在 `name` 之后)——这样新增这个字段时,
/// 既有全部positional 构造调用只需在末尾补一个参数,不必重排前 19 个参数(Task 12/13/14/15
/// 已写的测试构造调用均按此追加)。
public record AchievementDefinition(
        String code,
        String seriesCode,
        Integer tierLevel,
        String category,
        String rarity,
        int nbPoints,
        boolean hiddenReveal,
        boolean alwaysPrivate,
        String status,
        String predicateKind,
        String metricCode,
        BigDecimal thresholdValue,
        String comparator,
        String prerequisite,
        LocalDate launchDate,
        int sortOrder,
        String name,
        String flavorText,
        String hiddenHintText,
        String conditionText) {
}
