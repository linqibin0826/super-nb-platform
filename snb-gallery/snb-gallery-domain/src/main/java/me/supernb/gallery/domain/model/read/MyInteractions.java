package me.supernb.gallery.domain.model.read;

import java.util.List;

/// 批量互动态(这批 id 里我赞了/藏了哪些)。
public record MyInteractions(List<Long> liked, List<Long> favorited) {
}
