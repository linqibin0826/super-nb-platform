package me.supernb.gallery.domain.model.read;

import java.util.List;

/// 批量互动态:给一批提示词 id,回当前用户在这批里点过赞/收藏过的子集。
///
/// @param liked     这批 id 中当前用户已点赞的提示词 id
/// @param favorited 这批 id 中当前用户已收藏的提示词 id
public record MyInteractions(List<String> liked, List<String> favorited) {
}
