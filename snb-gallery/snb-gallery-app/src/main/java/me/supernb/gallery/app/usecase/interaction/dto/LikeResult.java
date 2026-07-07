package me.supernb.gallery.app.usecase.interaction.dto;

/// 点赞结果(计数 + 当前态)。
public record LikeResult(int likeCount, boolean liked) {
}
