package me.supernb.activity.adapter.rest.response;

import java.util.List;
import me.supernb.activity.domain.model.achievement.AchievementCategoryView;
import me.supernb.activity.domain.model.achievement.AchievementItemView;
import me.supernb.activity.domain.model.achievement.AchievementSeriesView;
import me.supernb.activity.domain.model.achievement.AchievementStepView;
import me.supernb.activity.domain.model.achievement.AchievementWallView;

/// GET /activity/v1/checkin/achievements 响应体,字段形状按前端接线计划契约总览逐一对齐。
public record AchievementWallResponse(
        SummaryLine summary,
        List<CategoryLine> categories,
        List<ItemLine> metaAchievements,
        List<String> pendingUnseal) {

    public static AchievementWallResponse of(AchievementWallView v) {
        return new AchievementWallResponse(
                new SummaryLine(v.summary().unlockedCount(), v.summary().totalCount(), v.summary().nbTotal()),
                v.categories().stream().map(AchievementWallResponse::categoryOf).toList(),
                v.metaAchievements().stream().map(AchievementWallResponse::itemOf).toList(),
                v.pendingUnseal());
    }

    private static CategoryLine categoryOf(AchievementCategoryView c) {
        return new CategoryLine(c.name(), c.hidden(),
                c.items().stream().map(AchievementWallResponse::itemOf).toList(),
                c.series().stream().map(AchievementWallResponse::seriesOf).toList());
    }

    private static SeriesLine seriesOf(AchievementSeriesView s) {
        ProgressLine progress = s.progress() == null ? null
                : new ProgressLine(s.progress().text(), s.progress().pct());
        return new SeriesLine(s.seriesName(), progress, s.steps().stream().map(AchievementWallResponse::stepOf).toList());
    }

    private static ItemLine itemOf(AchievementItemView i) {
        ProgressLine progress = i.progress() == null ? null
                : new ProgressLine(i.progress().text(), i.progress().pct());
        return new ItemLine(i.code(), i.name(), i.condition(), i.tier(), i.nbPoints(), i.unlocked(), i.status(),
                i.hiddenReveal(), i.flavorText(), i.linkUrl(), i.exclusiveTag(), i.revealedLabel(), progress,
                i.hint());
    }

    private static StepLine stepOf(AchievementStepView s) {
        return new StepLine(s.code(), s.name(), s.condition(), s.tier(), s.nbPoints(), s.unlocked(), s.status(),
                s.hiddenReveal(), s.flavorText());
    }

    public record SummaryLine(int unlockedCount, int totalCount, int nbTotal) {
    }

    public record ProgressLine(String text, int pct) {
    }

    public record ItemLine(String code, String name, String condition, int tier, int nbPoints, boolean unlocked,
            String status, boolean hiddenReveal, String flavorText, String linkUrl, String exclusiveTag,
            String revealedLabel, ProgressLine progress, String hint) {
    }

    public record StepLine(String code, String name, String condition, int tier, int nbPoints, boolean unlocked,
            String status, boolean hiddenReveal, String flavorText) {
    }

    public record SeriesLine(String seriesName, ProgressLine progress, List<StepLine> steps) {
    }

    public record CategoryLine(String name, boolean hidden, List<ItemLine> items, List<SeriesLine> series) {
    }
}
