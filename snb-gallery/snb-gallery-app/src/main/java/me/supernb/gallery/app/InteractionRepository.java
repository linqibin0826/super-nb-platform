package me.supernb.gallery.app;

import java.util.List;
import java.util.OptionalInt;

/// 点赞/收藏仓储端口(gallery 库)。toggle 幂等 + 反规范化计数 ±1,只认 published。
public interface InteractionRepository {

    /// 点赞开关(on=true 赞/false 取消),回最新 like_count;目标不存在/未发布 → empty(服务转 404)。
    OptionalInt toggleLike(long promptId, long userId, boolean on);

    /// 收藏开关,回最新 fav_count;目标不存在/未发布 → empty。
    OptionalInt toggleFavorite(long promptId, long userId, boolean on);

    /// 我的收藏分页(收藏时间倒序,同列表瘦身字段)。
    GalleryDto.Page<GalleryDto.PromptSummary> myFavorites(long userId, int page, int pageSize);

    /// 这批 id 里我赞了/藏了哪些。
    GalleryDto.MyInteractions myInteractions(List<Long> promptIds, long userId);
}
