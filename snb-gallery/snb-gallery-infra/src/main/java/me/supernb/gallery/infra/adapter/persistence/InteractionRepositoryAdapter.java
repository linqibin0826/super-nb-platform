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

/// 点赞/收藏聚合仓储适配器:成员表写入 + 反规范化计数维护,全部动作进一个事务。
///
/// 幂等策略:插入撞成员唯一约束 `UNIQUE(prompt_id, user_id)` → 整事务回滚 →
/// 外层回读当前计数;退出用 `@Modifying` 批量 DELETE,按行数决定是否减计数。
@Repository
public class InteractionRepositoryAdapter implements InteractionRepository {

    private static final String PUBLISHED = "published";

    private final PromptJpaRepository prompts;
    private final PromptLikeJpaRepository likes;
    private final PromptFavoriteJpaRepository favorites;
    private final TransactionTemplate txTemplate;

    /// 构造:注入三张表的 Spring Data 仓储与事务模板。
    public InteractionRepositoryAdapter(PromptJpaRepository prompts, PromptLikeJpaRepository likes,
                              PromptFavoriteJpaRepository favorites, PlatformTransactionManager txManager) {
        this.prompts = prompts;
        this.likes = likes;
        this.favorites = favorites;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /// 点赞/取消点赞,返回最新点赞数;条目不存在或未发布返回 empty。
    @Override
    public OptionalInt toggleLike(long promptId, long userId, boolean on) {
        try {
            return txTemplate.execute(status -> doToggleLike(promptId, userId, on));
        } catch (DataIntegrityViolationException e) {
            return currentCount(prompts.likeCountOf(promptId));
        }
    }

    /// 收藏/取消收藏,返回最新收藏数;条目不存在或未发布返回 empty。
    @Override
    public OptionalInt toggleFavorite(long promptId, long userId, boolean on) {
        try {
            return txTemplate.execute(status -> doToggleFavorite(promptId, userId, on));
        } catch (DataIntegrityViolationException e) {
            return currentCount(prompts.favCountOf(promptId));
        }
    }

    /// 事务体:点赞成员增删 + 计数增减(幂等:已存在不重复插,删 0 行不减)。
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

    /// 事务体:收藏成员增删 + 计数增减(幂等语义同点赞)。
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

    /// 把仓储的 Optional 计数折叠为 OptionalInt。
    private static OptionalInt currentCount(Optional<Integer> count) {
        return count.map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    /// 「我的收藏」分页:按收藏时刻倒序,只含仍在发布中的条目。
    @Override
    public Page<PromptSummary> myFavorites(long userId, int page, int pageSize) {
        org.springframework.data.domain.Page<PromptEntity> rows = favorites.favoritePrompts(userId, PageRequest.of(page - 1, pageSize));
        return Page.of(
                rows.getContent().stream().map(PromptMapper::toSummary).toList(),
                rows.getTotalElements(), page, pageSize);
    }

    /// 批量回填:给定条目集合中当前用户点赞/收藏过的 id 清单。
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
