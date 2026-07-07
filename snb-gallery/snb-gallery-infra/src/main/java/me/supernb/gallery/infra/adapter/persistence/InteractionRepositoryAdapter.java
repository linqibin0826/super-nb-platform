package me.supernb.gallery.infra.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import me.supernb.gallery.domain.model.read.MyInteractions;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.repository.InteractionRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.PromptFavoriteJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.PromptJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.dao.PromptLikeJpaRepository;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptFavoriteEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptLikeEntity;
import me.supernb.gallery.infra.adapter.read.PromptMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// InteractionRepository 实现:成员表写入与反规范化计数维护全部收在一个事务内。
///
/// 幂等策略:插入撞成员表唯一约束 `UNIQUE(prompt_id, user_id)` 时整个事务回滚,
/// 在事务外回读当前计数返回;退出成员靠 `@Modifying` 批量 DELETE,按返回行数决定是否联动减计数。
@Repository
public class InteractionRepositoryAdapter implements InteractionRepository {

    private static final String PUBLISHED = "published";

    private final PromptJpaRepository prompts;
    private final PromptLikeJpaRepository likes;
    private final PromptFavoriteJpaRepository favorites;
    private final TransactionTemplate txTemplate;

    /// 构造:注入提示词、点赞、收藏三个仓储,事务管理器内部包成 TransactionTemplate。
    public InteractionRepositoryAdapter(PromptJpaRepository prompts, PromptLikeJpaRepository likes,
                              PromptFavoriteJpaRepository favorites, PlatformTransactionManager txManager) {
        this.prompts = prompts;
        this.likes = likes;
        this.favorites = favorites;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /// 点赞开关(on=true 点赞、false 取消),返回最新点赞数;条目不存在或未发布返回 empty。
    /// 并发插入撞成员唯一约束时捕获 DataIntegrityViolationException,直接回读当前计数返回,不重试插入。
    @Override
    public OptionalInt toggleLike(long promptId, long userId, boolean on) {
        try {
            return txTemplate.execute(status -> doToggleLike(promptId, userId, on));
        } catch (DataIntegrityViolationException e) {
            return currentCount(prompts.likeCountOf(promptId));
        }
    }

    /// 收藏开关(on=true 收藏、false 取消),返回最新收藏数;条目不存在或未发布返回 empty。
    /// 并发插入撞成员唯一约束时捕获 DataIntegrityViolationException,直接回读当前计数返回,不重试插入。
    @Override
    public OptionalInt toggleFavorite(long promptId, long userId, boolean on) {
        try {
            return txTemplate.execute(status -> doToggleFavorite(promptId, userId, on));
        } catch (DataIntegrityViolationException e) {
            return currentCount(prompts.favCountOf(promptId));
        }
    }

    /// 事务体:点赞成员行增删 + 计数原子增减;已存在则不重复插入,删除影响 0 行则不联动减计数。
    private OptionalInt doToggleLike(long promptId, long userId, boolean on) {
        if (!prompts.existsByIdAndStatus(promptId, PUBLISHED)) {
            return OptionalInt.empty();
        }
        if (on) {
            if (!likes.existsByPromptIdAndUserId(promptId, userId)) {
                likes.save(new PromptLikeEntity(promptId, userId));
                prompts.adjustLikeCount(promptId, 1);
            }
        } else if (likes.deleteMembership(promptId, userId) > 0) {
            prompts.adjustLikeCount(promptId, -1);
        }
        return currentCount(prompts.likeCountOf(promptId));
    }

    /// 事务体:收藏成员行增删 + 计数原子增减,幂等语义同点赞。
    private OptionalInt doToggleFavorite(long promptId, long userId, boolean on) {
        if (!prompts.existsByIdAndStatus(promptId, PUBLISHED)) {
            return OptionalInt.empty();
        }
        if (on) {
            if (!favorites.existsByPromptIdAndUserId(promptId, userId)) {
                favorites.save(new PromptFavoriteEntity(promptId, userId));
                prompts.adjustFavCount(promptId, 1);
            }
        } else if (favorites.deleteMembership(promptId, userId) > 0) {
            prompts.adjustFavCount(promptId, -1);
        }
        return currentCount(prompts.favCountOf(promptId));
    }

    /// 把计数查询的 Optional 折叠为 OptionalInt,供点赞/收藏两条回读路径共用。
    private static OptionalInt currentCount(Optional<Integer> count) {
        return count.map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    /// 「我的收藏」分页:按收藏时刻倒序,只含仍在发布中的条目;
    /// Spring Data 的 Page 查询结果映射为读视图 Page 返回。
    @Override
    public Page<PromptSummary> myFavorites(long userId, int page, int pageSize) {
        org.springframework.data.domain.Page<PromptEntity> rows = favorites.favoritePrompts(userId, PageRequest.of(page - 1, pageSize));
        return Page.of(
                rows.getContent().stream().map(PromptMapper::toSummary).toList(),
                rows.getTotalElements(), page, pageSize);
    }

    /// 批量回填:给定 id 集合中当前用户点赞过/收藏过的子集,各自转字符串 id(对外 id 契约);
    /// 空集合直接短路返回两个空列表,不查库。
    @Override
    public MyInteractions myInteractions(List<Long> promptIds, long userId) {
        if (promptIds.isEmpty()) {
            return new MyInteractions(List.of(), List.of());
        }
        List<String> liked = likes.findByUserIdAndPromptIdIn(userId, promptIds).stream()
                .map(l -> String.valueOf(l.getPromptId())).toList();
        List<String> favorited = favorites.findByUserIdAndPromptIdIn(userId, promptIds).stream()
                .map(f -> String.valueOf(f.getPromptId())).toList();
        return new MyInteractions(liked, favorited);
    }
}
