package me.supernb.activity.domain.model.achievement;

/// 进度条视图(系列级"下一个未达标层级"进度,如 {"6,842 / 10,000", 68})。
public record ProgressView(String text, int pct) {
}
