package me.supernb.activity.domain.model.achievement;

import java.util.List;

/// 一条系列(如"调用量系列"),整条系列共享一个 progress 进度条(不是每层各自一条)。
public record AchievementSeriesView(String seriesName, ProgressView progress, List<AchievementStepView> steps) {
}
