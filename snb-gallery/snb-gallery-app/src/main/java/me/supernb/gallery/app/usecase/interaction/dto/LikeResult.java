package me.supernb.gallery.app.usecase.interaction.dto;

/// 点赞开关命令的写结果。
///
/// @param likeCount 操作后该提示词的最新点赞数
/// @param liked     操作后当前用户对该提示词的点赞状态(true=已点赞)
public record LikeResult(int likeCount, boolean liked) {
}
