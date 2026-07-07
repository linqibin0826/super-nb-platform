package me.supernb.gallery.domain.port.repository;

import java.util.List;
import java.util.OptionalInt;
import me.supernb.gallery.domain.model.read.MyInteractions;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptSummary;

/// 点赞/收藏仓储端口(gallery 库):成员关系(赞/藏)与反规范化计数(±1)的读写,
/// toggle 语义幂等,只认状态为 published 的提示词。
public interface InteractionRepository {

    /// 点赞开关:on=true 建立点赞、false 取消,返回最新点赞数;目标不存在或未发布 → empty(由用例转 404)。
    OptionalInt toggleLike(long promptId, long userId, boolean on);

    /// 收藏开关,语义同 [#toggleLike];返回最新收藏数,目标不存在或未发布 → empty。
    OptionalInt toggleFavorite(long promptId, long userId, boolean on);

    /// 我的收藏分页,按收藏时刻倒序,复用 [PromptSummary] 的列表瘦身字段。
    Page<PromptSummary> myFavorites(long userId, int page, int pageSize);

    /// 批量回填:这批提示词 id 里,当前用户点过赞/收藏过哪些。
    MyInteractions myInteractions(List<Long> promptIds, long userId);
}
