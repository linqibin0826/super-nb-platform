package me.supernb.gallery.app.usecase.interaction.dto;

/// 收藏开关命令的写结果。
///
/// @param favCount  操作后该提示词的最新收藏数
/// @param favorited 操作后当前用户对该提示词的收藏状态(true=已收藏)
public record FavResult(int favCount, boolean favorited) {
}
