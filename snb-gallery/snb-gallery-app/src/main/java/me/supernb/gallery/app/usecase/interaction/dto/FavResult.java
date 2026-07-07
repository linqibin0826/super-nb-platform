package me.supernb.gallery.app.usecase.interaction.dto;

/// 收藏结果(计数 + 当前态)。
public record FavResult(int favCount, boolean favorited) {
}
